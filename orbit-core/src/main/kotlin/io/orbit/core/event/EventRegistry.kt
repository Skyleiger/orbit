package io.orbit.core.event

import kotlin.reflect.KClass

interface EventRegistry {
    val definitions: List<EventDefinition>

    fun <E : Any> findEvent(eventType: EventType): EventDefinition?

    fun <E : Any> findEvent(eventClass: KClass<E>): EventDefinition?
}

internal class DefaultEventRegistry(
    override val definitions: List<EventDefinition>,
) : EventRegistry {
    private val definitionsByType: Map<EventType, EventDefinition> = definitions.associateBy { it.eventType }
    private val definitionsByClass: Map<KClass<*>, EventDefinition> = definitions.associateBy { it.eventClass }

    override fun <E : Any> findEvent(eventType: EventType): EventDefinition? = definitionsByType[eventType]

    override fun <E : Any> findEvent(eventClass: KClass<E>): EventDefinition? = definitionsByClass[eventClass]
}
