package io.orbit.core.handler

fun interface EventHandler<E> {
    fun onEvent(event: E)
}
