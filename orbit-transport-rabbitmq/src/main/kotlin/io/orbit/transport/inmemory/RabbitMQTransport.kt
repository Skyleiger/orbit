package io.orbit.transport.inmemory

import io.orbit.core.transport.MessageHandler
import io.orbit.core.transport.MessageTransport
import io.orbit.core.transport.TransportMessage

class RabbitMQTransport : MessageTransport {
    override suspend fun connect() {
        TODO("Not yet implemented")
    }

    override suspend fun disconnect() {
        TODO("Not yet implemented")
    }

    override suspend fun isConnected(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun send(message: TransportMessage) {
        TODO("Not yet implemented")
    }

    override suspend fun subscribe(
        eventType: String,
        handler: MessageHandler,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun unsubscribe(eventType: String) {
        TODO("Not yet implemented")
    }
}
