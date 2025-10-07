package io.orbit.core.transport

interface MessageTransport {
    suspend fun send(
        destination: String,
        message: TransportMessage,
    )

    fun subscribe(
        source: String,
        handler: MessageHandler,
    )
}
