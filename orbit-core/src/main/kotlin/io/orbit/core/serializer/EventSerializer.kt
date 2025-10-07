package io.orbit.core.serializer

import io.orbit.core.event.EventEnvelope

interface EventSerializer {
    fun <T : Any> serialize(envelope: EventEnvelope<T>): ByteArray

    fun <T : Any> deserialize(
        data: ByteArray,
        eventType: Class<T>,
    ): EventEnvelope<T>
}
