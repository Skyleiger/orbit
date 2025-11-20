package io.orbit.core.serializer

data class SerializedEvent(
    val data: ByteArray,
    val contentType: String,
    val contentEncoding: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SerializedEvent) return false

        if (!data.contentEquals(other.data)) return false
        if (contentType != other.contentType) return false
        if (contentEncoding != other.contentEncoding) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + contentEncoding.hashCode()
        return result
    }
}
