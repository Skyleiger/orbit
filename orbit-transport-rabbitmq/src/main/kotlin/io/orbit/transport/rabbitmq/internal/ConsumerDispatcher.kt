package io.orbit.transport.rabbitmq.internal

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import io.orbit.core.event.EventType
import io.orbit.core.transport.MessageHandler
import io.orbit.core.transport.TransportMessage
import io.orbit.transport.rabbitmq.RabbitMQTransportConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid

/**
 * Dispatches received RabbitMQ messages to registered handlers.
 *
 * This class is responsible for:
 * - Starting and stopping the RabbitMQ consumer
 * - Managing handler registrations per event type
 * - Dispatching incoming messages to handlers using coroutines
 * - Acknowledging or rejecting messages based on handler success
 *
 * ## Message Flow
 *
 * 1. RabbitMQ delivers a message to the consumer callback
 * 2. A coroutine is launched to process the message
 * 3. The message is converted to [TransportMessage]
 * 4. All handlers for the event type are invoked
 * 5. On success: message is acknowledged (ACK)
 * 6. On failure: message is rejected and requeued (NACK)
 *
 * ## Thread Safety
 *
 * Handler registration uses [ConcurrentHashMap] for thread-safe operations.
 * Message dispatch is performed in coroutines from the provided [CoroutineScope].
 *
 * ## Lifecycle
 *
 * Handlers are cleared via [clearHandlers] during disconnect.
 * Handler re-registration after reconnection is managed by Orbit core.
 * The coroutine scope remains active across reconnections.
 *
 * @param config The RabbitMQ transport configuration (for prefetch count).
 * @param coroutineScope The scope for launching message handler coroutines.
 *
 * @see RabbitMQTransportConfig.prefetchCount
 */
internal class ConsumerDispatcher(
    private val config: RabbitMQTransportConfig,
    private val coroutineScope: CoroutineScope,
) {
    private val handlers = ConcurrentHashMap<EventType, MutableList<MessageHandler>>()
    private var consumerTag: String? = null

    /**
     * Starts consuming messages from the specified queue.
     *
     * This method:
     * 1. Sets the prefetch count (QoS) for flow control
     * 2. Starts consuming with manual acknowledgment
     *
     * @param channel The RabbitMQ channel to consume from.
     * @param queueName The name of the queue to consume.
     */
    suspend fun startConsuming(
        channel: Channel,
        queueName: String,
    ) {
        withContext(Dispatchers.IO) {
            channel.basicQos(config.prefetchCount)

            consumerTag =
                channel.basicConsume(
                    queueName,
                    false,
                    createDeliverCallback(channel),
                    createCancelCallback(),
                )
        }
    }

    /**
     * Registers a handler for a specific event type.
     *
     * Multiple handlers can be registered for the same event type.
     * All handlers will be invoked for each message.
     *
     * @param eventType The event type (routing key) to handle.
     * @param handler The handler to invoke for messages.
     */
    fun registerHandler(
        eventType: EventType,
        handler: MessageHandler,
    ) {
        handlers.computeIfAbsent(eventType) { mutableListOf() }.add(handler)
    }

    /**
     * Removes all handlers for a specific event type.
     *
     * @param eventType The event type to remove handlers for.
     */
    fun unregisterHandlers(eventType: EventType) {
        handlers.remove(eventType)
    }

    /**
     * Clears all registered handlers.
     *
     * This method is called during disconnect to clean up state.
     * Handler re-registration after reconnect is managed by Orbit core.
     */
    fun clearHandlers() {
        handlers.clear()
    }

    private fun createDeliverCallback(channel: Channel) =
        DeliverCallback { _, delivery ->
            coroutineScope.launch {
                try {
                    val message = delivery.toTransportMessage()
                    dispatchToHandlers(message)

                    withContext(Dispatchers.IO) {
                        channel.basicAck(delivery.envelope.deliveryTag, false)
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.IO) {
                        channel.basicNack(delivery.envelope.deliveryTag, false, true)
                    }
                }
            }
        }

    private fun createCancelCallback() = CancelCallback { }

    private suspend fun dispatchToHandlers(message: TransportMessage) {
        val eventType = EventType(message.eventType)
        handlers[eventType]?.forEach { handler ->
            handler.handle(message)
        }
    }
}

/**
 * Converts a RabbitMQ [Delivery] to a [TransportMessage].
 *
 * Missing properties are filled with defaults:
 * - `messageId`: Random UUID if not set
 * - `contentType`: `application/octet-stream` if not set
 * - `contentEncoding`: `utf-8` if not set
 */
private fun Delivery.toTransportMessage() =
    TransportMessage(
        messageId = properties.messageId ?: Uuid.random().toString(),
        eventType = envelope.routingKey,
        contentType = properties.contentType ?: "application/octet-stream",
        contentEncoding = properties.contentEncoding ?: "utf-8",
        body = body,
    )
