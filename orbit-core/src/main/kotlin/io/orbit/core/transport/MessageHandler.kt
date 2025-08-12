package io.orbit.core.transport

fun interface MessageHandler {
  fun onMessage(message: TransportMessage)
}