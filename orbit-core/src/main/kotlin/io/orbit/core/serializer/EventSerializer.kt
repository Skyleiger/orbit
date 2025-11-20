package io.orbit.core.serializer

import io.orbit.core.event.EventEnvelope

interface EventSerializer {
    fun <T : Any> serialize(envelope: EventEnvelope<T>): SerializedEvent

    fun <T : Any> deserialize(
        serialized: SerializedEvent,
        eventClass: Class<T>,
    ): EventEnvelope<T>
}
