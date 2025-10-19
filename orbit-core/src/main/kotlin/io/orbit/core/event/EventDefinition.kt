package io.orbit.core.event

import kotlin.reflect.KClass

interface EventDefinition {
    val eventType: EventType

    val eventClass: KClass<*>
}

internal data class DefaultEventDefinition(
    override val eventType: EventType,
    override val eventClass: KClass<*>,
) : EventDefinition
