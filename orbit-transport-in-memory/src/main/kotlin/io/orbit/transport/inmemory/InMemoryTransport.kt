package io.orbit.transport.inmemory

import io.orbit.core.transport.MessageHandler
import io.orbit.core.transport.MessageTransport
import io.orbit.core.transport.TransportMessage

class InMemoryTransport : MessageTransport {
  private val handlers = mutableMapOf<String, MutableList<MessageHandler>>()

  override suspend fun send(destination: String, message: TransportMessage) {
    handlers[destination]?.forEach { handler ->
      try {
        handler.onMessage(message)
      } catch (e: Exception) {
        println("Error in handler: ${e.message}")
      }
    }
  }

  override fun subscribe(source: String, handler: MessageHandler) {
    handlers.computeIfAbsent(source) { mutableListOf() }.add(handler)
  }
}