package io.orbit.transport.rabbitmq

import io.orbit.core.event.EventType
import io.orbit.core.service.ServiceIdentity
import io.orbit.core.transport.MessageHandler
import io.orbit.core.transport.MessageTransport
import io.orbit.core.transport.TransportMessage
import io.orbit.transport.rabbitmq.internal.ConnectionManager
import io.orbit.transport.rabbitmq.internal.ConsumerDispatcher
import io.orbit.transport.rabbitmq.internal.QueueManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren

/**
 * RabbitMQ implementation of the [MessageTransport] interface.
 *
 * This transport adapter uses RabbitMQ as the messaging infrastructure for
 * publishing and consuming events. It implements a topic-based routing pattern
 * where:
 *
 * - A single **topic exchange** (default: `orbit.events`) is used for all events
 * - Each service instance creates its own **dedicated queue** (named after [ServiceIdentity.source])
 * - Event subscriptions create **queue bindings** with the event type as the routing key
 *
 * ## Architecture
 *
 * ```
 * ┌─────────────────────────────────────────────────────────┐
 * │           Exchange: orbit.events (TOPIC)                │
 * └─────────────────────────────────────────────────────────┘
 *         │ user.created    │ order.placed    │ ...
 *         ▼                 ▼                 ▼
 * ┌───────────────┐ ┌───────────────┐ ┌───────────────┐
 * │ Queue:        │ │ Queue:        │ │ Queue:        │
 * │ user-svc-xxx  │ │ order-svc-yyy │ │ analytics-zzz │
 * └───────────────┘ └───────────────┘ └───────────────┘
 * ```
 *
 * ## Metadata
 *
 * The transport enriches queues, connections, and messages with Orbit metadata:
 *
 * - **Connection properties**: `product`, `version`, `platform`, `service_name`, `service_id`
 * - **Queue arguments**: `x-orbit-version`, `x-orbit-service-name`, `x-orbit-service-id`, `x-orbit-created-at`
 * - **Message headers**: `x-orbit-source`, `x-orbit-version`
 *
 * ## Example Usage
 *
 * ```kotlin
 * val config = RabbitMQTransportConfig(
 *     host = "localhost",
 *     port = 5672,
 *     exchangeName = "my-app.events"
 * )
 *
 * val transport = RabbitMQTransport(
 *     config = config,
 *     serviceIdentity = ServiceIdentity("my-service")
 * )
 *
 * transport.connect()
 *
 * // Subscribe to events
 * transport.subscribe("user.created") { message ->
 *     println("Received: ${message.body.decodeToString()}")
 * }
 *
 * // Publish events
 * transport.send(TransportMessage(
 *     messageId = "123",
 *     eventType = "user.created",
 *     contentType = "application/json",
 *     contentEncoding = "utf-8",
 *     body = """{"userId": 1}""".toByteArray()
 * ))
 *
 * transport.disconnect()
 * ```
 *
 * @param config The RabbitMQ connection and messaging configuration.
 * @param serviceIdentity The identity of this service instance, used to generate
 *                        unique queue names and metadata.
 *
 * @see RabbitMQTransportConfig
 * @see MessageTransport
 * @see ServiceIdentity
 */
class RabbitMQTransport(
    private val config: RabbitMQTransportConfig,
    private val serviceIdentity: ServiceIdentity,
) : MessageTransport {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val connectionManager = ConnectionManager(config, serviceIdentity)
    private val queueManager = QueueManager(config, serviceIdentity)
    private val consumerDispatcher = ConsumerDispatcher(config, coroutineScope)

    /**
     * Establishes a connection to the RabbitMQ server.
     *
     * This method performs the following steps:
     * 1. Opens a connection to RabbitMQ with Orbit metadata in client properties
     * 2. Declares the topic exchange (idempotent operation)
     * 3. Declares the service-specific queue with Orbit metadata in arguments
     * 4. Starts the message consumer
     *
     * The transport can be reconnected after disconnect() by calling this method again.
     * After reconnection, handlers must be re-registered via subscribe() (managed by Orbit core).
     *
     * @throws com.rabbitmq.client.AlreadyClosedException if the connection fails
     */
    override suspend fun connect() {
        val channel = connectionManager.connect()

        queueManager.declareEventsExchange(channel)
        queueManager.declareServiceQueue(channel)

        consumerDispatcher.startConsuming(channel, queueManager.queueName)
    }

    /**
     * Closes the connection to RabbitMQ and releases all resources.
     *
     * This method:
     * 1. Clears all handler registrations and queue bindings
     * 2. Closes the channel and connection (which automatically cancels the consumer)
     * 3. Cancels all pending coroutines in the scope
     *
     * The transport can be reconnected by calling [connect] again.
     * The coroutine scope remains active and can be reused.
     * Handler re-registration is managed by the Orbit core.
     */
    override suspend fun disconnect() {
        queueManager.clearBindings()
        consumerDispatcher.clearHandlers()
        connectionManager.disconnect()
        coroutineScope.coroutineContext[Job]?.cancelChildren()
    }

    /**
     * Checks if the transport is currently connected to RabbitMQ.
     *
     * @return `true` if both the connection and channel are open, `false` otherwise.
     */
    override suspend fun isConnected(): Boolean = connectionManager.isConnected()

    /**
     * Publishes a message to the RabbitMQ exchange.
     *
     * The message is published to the configured exchange with:
     * - **Routing key**: [TransportMessage.eventType]
     * - **Headers**: Orbit metadata (`x-orbit-source`, `x-orbit-version`)
     *
     * @param message The transport message to send.
     * @throws IllegalStateException if the transport is not connected.
     */
    override suspend fun send(message: TransportMessage) {
        val channel =
            connectionManager.channel
                ?: error("Transport not connected")

        queueManager.publish(channel, message, serviceIdentity)
    }

    /**
     * Subscribes to messages of a specific event type.
     *
     * This creates a queue binding from the service queue to the exchange
     * with the event type as the routing key. All messages published with
     * this routing key will be delivered to the handler.
     *
     * Multiple handlers can be registered for the same event type.
     *
     * @param eventType The event type to subscribe to (used as routing key).
     * @param handler The handler to invoke for each received message.
     * @throws IllegalStateException if the transport is not connected.
     */
    override suspend fun subscribe(
        eventType: EventType,
        handler: MessageHandler,
    ) {
        val channel =
            connectionManager.channel
                ?: error("Transport not connected")

        queueManager.bindEventType(channel, eventType)
        consumerDispatcher.registerHandler(eventType, handler)
    }

    /**
     * Unsubscribes from messages of a specific event type.
     *
     * This removes the queue binding and deregisters all handlers
     * for the specified event type.
     *
     * @param eventType The event type to unsubscribe from.
     * @throws IllegalStateException if the transport is not connected.
     */
    override suspend fun unsubscribe(eventType: EventType) {
        val channel =
            connectionManager.channel
                ?: error("Transport not connected")

        queueManager.unbindEventType(channel, eventType)
        consumerDispatcher.unregisterHandlers(eventType)
    }
}
