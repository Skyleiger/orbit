package io.orbit.core.transport

import kotlinx.coroutines.runBlocking

interface MessageTransport : AutoCloseable {
    suspend fun connect()

    suspend fun disconnect()

    suspend fun isConnected(): Boolean

    suspend fun send(message: TransportMessage)

    suspend fun subscribe(
        eventType: String,
        handler: MessageHandler,
    )

    suspend fun unsubscribe(eventType: String)

    override fun close() {
        runBlocking { disconnect() }
    }
}
