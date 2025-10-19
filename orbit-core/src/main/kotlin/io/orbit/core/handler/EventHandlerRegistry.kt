package io.orbit.core.handler

import io.orbit.core.event.EventType

interface EventHandlerRegistry {
    fun <E : Any> getHandlers(eventType: EventType): List<EventHandler<E>>
}

internal class DefaultEventHandlerRegistry(
    private val handlers: Map<EventType, List<EventHandler<*>>>,
) : EventHandlerRegistry {
    @Suppress("UNCHECKED_CAST")
    override fun <E : Any> getHandlers(eventType: EventType): List<EventHandler<E>> =
        handlers[eventType]?.toList() as? List<EventHandler<E>> ?: emptyList()
}
