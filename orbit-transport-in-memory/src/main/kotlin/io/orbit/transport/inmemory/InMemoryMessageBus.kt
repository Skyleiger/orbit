package io.orbit.transport.inmemory

import io.orbit.core.event.EventType
import io.orbit.core.service.ServiceId
import io.orbit.core.service.ServiceIdentity
import io.orbit.core.service.ServiceName
import io.orbit.core.transport.MessageHandler
import io.orbit.core.transport.TransportMessage
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Central message bus that coordinates message delivery between multiple
 * [InMemoryTransport] instances.
 *
 * This allows multiple Orbit instances to communicate through separate
 * transport instances while sharing the same message infrastructure.
 *
 * Messages are delivered to a random instance of each subscribed service,
 * ensuring load balancing across service instances.
 */
class InMemoryMessageBus {
    private val subscribers =
        ConcurrentHashMap<ServiceName, ConcurrentHashMap<ServiceId, ConcurrentHashMap<EventType, MessageHandler>>>()

    /**
     * Publishes a message to one random instance per service.
     * This ensures load balancing across multiple instances of the same service.
     */
    suspend fun publish(message: TransportMessage) {
        val eventType = EventType(message.eventType)

        subscribers.values.forEach { instances ->
            instances.randomValueOrNull()?.let { handlers ->
                handlers[eventType]?.handle(message)
            }
        }
    }

    /**
     * Subscribes a handler to messages of a specific event type.
     */
    fun subscribe(
        serviceIdentity: ServiceIdentity,
        eventType: EventType,
        handler: MessageHandler,
    ) {
        val handlers =
            subscribers
                .computeIfAbsent(serviceIdentity.name) { ConcurrentHashMap() }
                .computeIfAbsent(serviceIdentity.id) { ConcurrentHashMap() }

        require(!handlers.containsKey(eventType)) { "Handler already subscribed for event type $eventType" }
        handlers[eventType] = handler
    }

    /**
     * Unsubscribes a handler from messages of a specific event type.
     */
    fun unsubscribe(
        serviceIdentity: ServiceIdentity,
        eventType: EventType,
    ) {
        subscribers[serviceIdentity.name]?.get(serviceIdentity.id)?.remove(eventType)
    }

    /**
     * Unsubscribes all handlers for a specific service instance.
     */
    fun unsubscribeAll(serviceIdentity: ServiceIdentity) {
        subscribers[serviceIdentity.name]?.remove(serviceIdentity.id)
        if (subscribers[serviceIdentity.name]?.isEmpty() == true) {
            subscribers.remove(serviceIdentity.name)
        }
    }

    private fun <K, V> ConcurrentHashMap<K & Any, V & Any>.randomValueOrNull(): V? {
        // Reservoir sampling: uniformly selects 1 element from an iterator,
        // without size/index (thus avoiding races with concurrent modifications) and without allocation.
        var chosen: V? = null
        var seen = 0
        for (v in values) {
            seen++
            if (Random.nextInt(seen) == 0) {
                chosen = v
            }
        }
        return chosen
    }
}
