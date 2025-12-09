package io.orbit.transport.inmemory

import io.orbit.core.event.EventType
import io.orbit.core.transport.MessageHandler
import io.orbit.core.transport.TransportMessage

/**
 * Central message bus that coordinates message delivery between multiple
 * [InMemoryTransport] instances.
 *
 * This allows multiple Orbit instances to communicate through separate
 * transport instances while sharing the same message infrastructure.
 */
class InMemoryMessageBus {
    private val subscribers = mutableMapOf<EventType, MutableList<MessageHandler>>()

    /**
     * Publishes a message to all subscribers of the given event type.
     */
    suspend fun publish(message: TransportMessage) {
        val eventType = EventType(message.eventType)
        subscribers[eventType]?.forEach { handler ->
            handler.handle(message)
        }
    }

    /**
     * Subscribes a handler to messages of a specific event type.
     */
    fun subscribe(
        eventType: EventType,
        handler: MessageHandler,
    ) {
        subscribers.computeIfAbsent(eventType) { mutableListOf() }.add(handler)
    }

    /**
     * Unsubscribes a handler from messages of a specific event type.
     */
    fun unsubscribe(
        eventType: EventType,
        handler: MessageHandler,
    ) {
        subscribers[eventType]?.remove(handler)
        if (subscribers[eventType]?.isEmpty() == true) {
            subscribers.remove(eventType)
        }
    }

    /**
     * Unsubscribes all handlers for a specific event type.
     */
    fun unsubscribeAll(eventType: EventType) {
        subscribers.remove(eventType)
    }
}
