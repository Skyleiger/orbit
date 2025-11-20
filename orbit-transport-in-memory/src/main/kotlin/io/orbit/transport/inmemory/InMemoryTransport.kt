package io.orbit.transport.inmemory

import io.orbit.core.transport.MessageHandler
import io.orbit.core.transport.MessageTransport
import io.orbit.core.transport.TransportMessage

class InMemoryTransport : MessageTransport {
    private val handlers = mutableMapOf<String, MutableList<MessageHandler>>()
    private var isConnected = false

    override suspend fun connect() {
        isConnected = true
    }

    override suspend fun disconnect() {
        isConnected = false
        handlers.clear()
    }

    override suspend fun isConnected(): Boolean = isConnected

    override suspend fun send(message: TransportMessage) {
        checkIsConnected()

        handlers[message.eventType]?.forEach { handler ->
            handler.handle(message)
        }
    }

    override suspend fun subscribe(
        eventType: String,
        handler: MessageHandler,
    ) {
        checkIsConnected()
        handlers.computeIfAbsent(eventType) { mutableListOf() }.add(handler)
    }

    override suspend fun unsubscribe(eventType: String) {
        handlers.remove(eventType)
    }

    private suspend fun checkIsConnected() {
        check(isConnected()) { "Transport not connected" }
    }
}
