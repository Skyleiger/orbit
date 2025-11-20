package io.orbit.core.transport

data class TransportMessage(
    val messageId: String,
    val eventType: String,
    val contentType: String,
    val contentEncoding: String,
    val body: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransportMessage) return false

        if (messageId != other.messageId) return false
        if (eventType != other.eventType) return false
        if (contentType != other.contentType) return false
        if (contentEncoding != other.contentEncoding) return false
        if (!body.contentEquals(other.body)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = messageId.hashCode()
        result = 31 * result + eventType.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + contentEncoding.hashCode()
        result = 31 * result + body.contentHashCode()
        return result
    }
}
