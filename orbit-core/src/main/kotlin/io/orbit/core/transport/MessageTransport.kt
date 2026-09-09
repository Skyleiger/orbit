package io.orbit.core.transport

import io.orbit.core.event.EventType

/**
 * Interface for a messaging transport used by Orbit to publish and consume events.
 *
 * **Ordering:** delivery order is not guaranteed. Messages may be handled in an order different
 * from the order in which they were sent, and handlers may be invoked concurrently for different
 * messages. Events are load balanced across all instances of a service, so a message sent later
 * may be handled by another instance before an earlier one has completed. Handlers must therefore
 * be order-independent.
 */
interface MessageTransport {
    /**
     * Connects this transport.
     *
     * **Idempotent:** calling this method when already connected must be a no-op.
     *
     * @throws Exception if establishing the underlying connection fails (implementation-specific)
     */
    suspend fun connect()

    /**
     * Disconnects this transport and releases all underlying resources.
     *
     * **Idempotent:** calling this method when already disconnected must be a no-op.
     *
     * @throws Exception if disconnecting fails (implementation-specific)
     */
    suspend fun disconnect()

    /**
     * Returns whether this transport is currently connected and ready for operations.
     *
     * This method should not have side effects.
     *
     * @return `true` if connected, `false` otherwise
     */
    suspend fun isConnected(): Boolean

    /**
     * Sends a message via this transport.
     *
     * Requires an active connection.
     *
     * @throws IllegalStateException if the transport is not connected
     * @throws Exception if sending fails (implementation-specific)
     */
    suspend fun send(message: TransportMessage)

    /**
     * Subscribes to messages of the given [eventType] and registers the provided [handler].
     *
     * Requires an active connection.
     *
     * Exactly one handler may be registered per [EventType]. If a handler is already registered
     * for [eventType], implementations should throw an [IllegalStateException].
     *
     * @throws IllegalStateException if the transport is not connected
     * @throws IllegalStateException if a handler is already registered for [eventType]
     * @throws Exception if subscribing fails (implementation-specific)
     */
    suspend fun subscribe(
        eventType: EventType,
        handler: MessageHandler,
    )

    /**
     * Unsubscribes from messages of the given [eventType].
     *
     * Requires an active connection.
     *
     * **Idempotent:** calling this method multiple times for the same [eventType] must be a no-op.
     *
     * @throws IllegalStateException if the transport is not connected
     * @throws Exception if unsubscribing fails (implementation-specific)
     */
    suspend fun unsubscribe(eventType: EventType)
}
