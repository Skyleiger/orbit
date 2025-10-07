package io.orbit.core.transport

data class TransportMessage(
    val routingKey: String,
    val payload: ByteArray,
    val headers: Map<String, String> = emptyMap(),
)
