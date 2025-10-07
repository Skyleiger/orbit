package io.orbit.core

import kotlin.reflect.KClass

interface EventTypeRegistry {
    fun <E : Any> register(eventClass: KClass<E>)

    fun getEventClass(eventTypeName: String): KClass<out Any>?
}

inline fun <reified T : Any> EventTypeRegistry.register() {
    register(T::class)
}
