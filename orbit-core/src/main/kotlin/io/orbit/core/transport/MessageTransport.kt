package io.orbit.core.transport

import io.orbit.core.event.EventType
import kotlinx.coroutines.runBlocking

interface MessageTransport : AutoCloseable {
    suspend fun connect()

    suspend fun disconnect()

    suspend fun isConnected(): Boolean

    suspend fun send(message: TransportMessage)

    suspend fun subscribe(
        eventType: EventType,
        handler: MessageHandler,
    )

    suspend fun unsubscribe(eventType: EventType)

    override fun close() {
        runBlocking { disconnect() }
    }
}
