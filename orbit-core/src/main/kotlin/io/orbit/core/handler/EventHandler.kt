package io.orbit.core.handler

fun interface EventHandler<E> {
    suspend fun handle(event: E)
}
