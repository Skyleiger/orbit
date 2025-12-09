package io.orbit.transport.rabbitmq.internal

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Channel
import io.orbit.core.Orbit
import io.orbit.core.event.EventType
import io.orbit.core.service.ServiceIdentity
import io.orbit.core.transport.TransportMessage
import io.orbit.transport.rabbitmq.RabbitMQTransportConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Date
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Manages RabbitMQ exchange, queue, and binding operations.
 *
 * This class is responsible for:
 * - Declaring the topic exchange for all events
 * - Declaring the service-specific queue with Orbit metadata
 * - Managing queue bindings for event subscriptions
 * - Publishing messages to the exchange
 *
 * ## Queue Naming
 *
 * The queue name is derived from [ServiceIdentity.source], resulting in a
 * format like `my-service-abc12345-...`. This ensures each service instance
 * has its own dedicated queue.
 *
 * ## Queue Arguments (Metadata)
 *
 * The queue is created with the following arguments visible in RabbitMQ:
 *
 * | Argument | Description |
 * |----------|-------------|
 * | `x-orbit-version` | Orbit version |
 * | `x-orbit-service-name` | The service name |
 * | `x-orbit-service-id` | The unique service instance ID |
 * | `x-orbit-created-at` | ISO 8601 timestamp of queue creation |
 *
 * ## Message Properties
 *
 * Published messages include:
 * - `messageId`: From [TransportMessage.messageId]
 * - `contentType`: From [TransportMessage.contentType]
 * - `contentEncoding`: From [TransportMessage.contentEncoding]
 * - `timestamp`: Current time
 * - Headers: `x-orbit-source`, `x-orbit-version`
 *
 * @param config The RabbitMQ transport configuration.
 * @param serviceIdentity The service identity for queue naming and metadata.
 *
 * @see RabbitMQTransportConfig
 */
internal class QueueManager(
    private val config: RabbitMQTransportConfig,
    private val serviceIdentity: ServiceIdentity,
) {
    private val bindings = CopyOnWriteArraySet<EventType>()

    /**
     * The unique queue name for this service instance.
     */
    val queueName: String
        get() = serviceIdentity.source

    /**
     * Declares the topic exchange for event routing.
     *
     * The exchange is declared as durable and non-auto-delete.
     * This operation is idempotent.
     *
     * @param channel The RabbitMQ channel to use.
     */
    suspend fun declareEventsExchange(channel: Channel) {
        withContext(Dispatchers.IO) {
            channel.exchangeDeclare(
                config.exchangeName,
                BuiltinExchangeType.TOPIC,
                true,
                false,
                null,
            )
        }
    }

    /**
     * Declares the service-specific queue with Orbit metadata.
     *
     * The queue is created with arguments containing Orbit metadata
     * for observability in the RabbitMQ management UI.
     *
     * @param channel The RabbitMQ channel to use.
     */
    suspend fun declareServiceQueue(channel: Channel) {
        val queueArguments = buildQueueArguments()

        withContext(Dispatchers.IO) {
            channel.queueDeclare(
                queueName,
                false, // non-durable: queue name is ephemeral (random UUID per instance)
                false, // non-exclusive: allow reconnection after recovery
                true, // auto-delete: clean up when connection closes
                queueArguments,
            )
        }
    }

    /**
     * Binds the service queue to the exchange for a specific event type.
     *
     * This enables the service to receive messages with the specified
     * routing key (event type). The binding is tracked to prevent
     * duplicate bindings.
     *
     * @param channel The RabbitMQ channel to use.
     * @param eventType The event type to bind (used as routing key).
     */
    suspend fun bindEventType(
        channel: Channel,
        eventType: EventType,
    ) {
        if (bindings.add(eventType)) {
            withContext(Dispatchers.IO) {
                channel.queueBind(
                    queueName,
                    config.exchangeName,
                    eventType.value,
                )
            }
        }
    }

    /**
     * Removes the binding for a specific event type.
     *
     * After unbinding, the service will no longer receive messages
     * with this routing key.
     *
     * @param channel The RabbitMQ channel to use.
     * @param eventType The event type to unbind.
     */
    suspend fun unbindEventType(
        channel: Channel,
        eventType: EventType,
    ) {
        if (bindings.remove(eventType)) {
            withContext(Dispatchers.IO) {
                channel.queueUnbind(
                    queueName,
                    config.exchangeName,
                    eventType.value,
                )
            }
        }
    }

    /**
     * Clears all queue bindings.
     *
     * This method is called during disconnect to clean up state.
     * Handler re-registration after reconnect is managed by Orbit core.
     */
    fun clearBindings() {
        bindings.clear()
    }

    /**
     * Publishes a message to the exchange.
     *
     * The message is published with:
     * - Routing key: [TransportMessage.eventType]
     * - AMQP properties including Orbit metadata headers
     *
     * @param channel The RabbitMQ channel to use.
     * @param message The transport message to publish.
     * @param serviceIdentity The service identity for metadata.
     */
    suspend fun publish(
        channel: Channel,
        message: TransportMessage,
        serviceIdentity: ServiceIdentity,
    ) {
        val properties = buildMessageProperties(message, serviceIdentity)

        withContext(Dispatchers.IO) {
            channel.basicPublish(
                config.exchangeName,
                message.eventType,
                properties,
                message.body,
            )
        }
    }

    private fun buildQueueArguments(): Map<String, Any> =
        mapOf(
            "x-orbit-version" to Orbit.VERSION,
            "x-orbit-service-name" to serviceIdentity.name.value,
            "x-orbit-service-id" to serviceIdentity.id.value,
            "x-orbit-created-at" to Instant.now().toString(),
        )

    private fun buildMessageProperties(
        message: TransportMessage,
        serviceIdentity: ServiceIdentity,
    ): AMQP.BasicProperties =
        AMQP.BasicProperties
            .Builder()
            .messageId(message.messageId)
            .contentType(message.contentType)
            .contentEncoding(message.contentEncoding)
            .timestamp(Date())
            .headers(
                mapOf(
                    "x-orbit-source" to serviceIdentity.source,
                    "x-orbit-version" to Orbit.VERSION,
                ),
            ).build()
}
