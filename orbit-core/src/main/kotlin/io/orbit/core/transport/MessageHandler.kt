package io.orbit.core.transport

fun interface MessageHandler {
    suspend fun handle(message: TransportMessage)
}
