package io.orbit.core.event

import kotlin.reflect.KClass

interface EventIntrospector {
    fun introspect(eventClass: KClass<*>): EventDefinition
}

class ReflectionEventIntrospector : EventIntrospector {
    override fun introspect(eventClass: KClass<*>): EventDefinition {
        TODO("Not yet implemented")
    }
}
