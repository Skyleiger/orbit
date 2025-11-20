package io.orbit.core.event

import kotlin.reflect.KClass

interface EventIntrospector {
    fun introspect(eventClass: KClass<*>): EventDefinition
}

class ReflectionEventIntrospector : EventIntrospector {
    override fun introspect(eventClass: KClass<*>): EventDefinition {
        val eventAnnotation =
            eventClass.annotations.find { it is Event } as? Event
                ?: error("Class ${eventClass.simpleName} must be annotated with @Event")

        return DefaultEventDefinition(
            eventType = EventType(eventAnnotation.type),
            eventClass = eventClass,
        )
    }
}
