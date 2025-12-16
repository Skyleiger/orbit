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
import java.util.Date
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.time.Clock

/**
 * Manages RabbitMQ exchange, queue, and binding operations.
 *
 * This class is responsible for:
 * - Declaring the topic exchange for all events
 * - Declaring the shared service queue with minimal metadata
 * - Managing queue bindings for event subscriptions
 * - Publishing messages to the exchange
 *
 * ## Queue Naming
 *
 * The queue name is derived from [ServiceIdentity.name], resulting in a
 * format like `email-service`. This ensures all instances of the same service
 * share a queue for load balancing.
 *
 * ## Queue Arguments (Metadata)
 *
 * For shared, durable queues, arguments are kept minimal to avoid stale metadata:
 *
 * | Argument | Description |
 * |----------|-------------|
 * | `x-orbit-managed` | Marker that this queue is managed by Orbit |
 * | `x-orbit-created-version` | Orbit version at queue creation |
 * | `x-orbit-created-at` | ISO 8601 timestamp of queue creation |
 *
 * Instance-specific metadata (version, instance ID) is propagated via
 * **connection properties** and **message headers** instead.
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
     * The queue name for this service.
     *
     * All instances of the same service share the same queue to enable
     * load balancing - each event is delivered to only one instance.
     */
    val queueName: String
        get() = serviceIdentity.name.value

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
     * The queue is shared across all instances of the same service to enable
     * load balancing. Each event is delivered to only one instance.
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
                true, // durable: queue survives broker restarts
                false, // non-exclusive: shared across service instances
                false, // auto-delete: no auto delete, so that messages are retained when there are no consumers
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
     * This method is called during disconnect to clean up the state.
     * Handler re-registration after reconnection is managed by Orbit core.
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
            "x-orbit-managed" to true,
            "x-orbit-created-version" to Orbit.VERSION,
            "x-orbit-created-at" to Clock.System.now().toString(), // ISO 8601
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
                    "x-orbit-revision" to Orbit.REVISION,
                    "x-orbit-version" to Orbit.VERSION,
                ),
            ).build()
}
