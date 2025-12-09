package io.orbit.transport.inmemory

import io.orbit.core.event.EventType
import io.orbit.core.transport.MessageHandler
import io.orbit.core.transport.MessageTransport
import io.orbit.core.transport.TransportMessage

/**
 * In-memory implementation of [MessageTransport] for testing and development.
 *
 * This transport uses an internal message bus to deliver messages between
 * different transport instances. Each [InMemoryTransport] instance maintains
 * its own connection state and handler registrations, but all instances
 * connected to the same message bus can communicate with each other.
 *
 * @param messageBus The shared message bus for inter-transport communication.
 *                   If not provided, creates an isolated bus for this instance only.
 *
 */
class InMemoryTransport(
    private val messageBus: InMemoryMessageBus = InMemoryMessageBus(),
) : MessageTransport {
    private val localHandlers = mutableMapOf<EventType, MutableList<MessageHandler>>()
    private var isConnected = false

    override suspend fun connect() {
        isConnected = true
    }

    override suspend fun disconnect() {
        isConnected = false
        // Unsubscribe all local handlers from the message bus
        localHandlers.forEach { (eventType, handlers) ->
            handlers.forEach { handler ->
                messageBus.unsubscribe(eventType, handler)
            }
        }
        localHandlers.clear()
    }

    override suspend fun isConnected(): Boolean = isConnected

    override suspend fun send(message: TransportMessage) {
        checkIsConnected()
        messageBus.publish(message)
    }

    override suspend fun subscribe(
        eventType: EventType,
        handler: MessageHandler,
    ) {
        checkIsConnected()
        localHandlers.computeIfAbsent(eventType) { mutableListOf() }.add(handler)
        messageBus.subscribe(eventType, handler)
    }

    override suspend fun unsubscribe(eventType: EventType) {
        localHandlers[eventType]?.forEach { handler ->
            messageBus.unsubscribe(eventType, handler)
        }
        localHandlers.remove(eventType)
    }

    private suspend fun checkIsConnected() {
        check(isConnected()) { "Transport not connected" }
    }
}
