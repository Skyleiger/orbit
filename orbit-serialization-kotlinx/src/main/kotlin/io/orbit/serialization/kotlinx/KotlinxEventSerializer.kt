package io.orbit.serialization.kotlinx

import io.orbit.core.event.EventEnvelope
import io.orbit.core.serializer.EventSerializer
import io.orbit.core.serializer.SerializedEvent
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.full.createType

class KotlinxEventSerializer(
    private val json: Json =
        Json {
            prettyPrint = false
            ignoreUnknownKeys = true
        },
) : EventSerializer {
    override fun <T : Any> serialize(envelope: EventEnvelope<T>): SerializedEvent {
        val eventClass = envelope.event::class
        val eventSerializer = serializer(eventClass.createType())

        @Suppress("UNCHECKED_CAST")
        val typedEventSerializer = eventSerializer as KSerializer<T>
        val envelopeSerializer = EventEnvelopeSerializer(typedEventSerializer)

        val jsonString = json.encodeToString(envelopeSerializer, envelope)
        return SerializedEvent(
            data = jsonString.toByteArray(Charsets.UTF_8),
            contentType = CONTENT_TYPE,
            contentEncoding = CONTENT_ENCODING,
        )
    }

    override fun <T : Any> deserialize(
        serialized: SerializedEvent,
        eventClass: Class<T>,
    ): EventEnvelope<T> {
        require(serialized.contentType == CONTENT_TYPE) {
            "Unsupported content type: ${serialized.contentType}. Expected: $CONTENT_TYPE"
        }
        require(serialized.contentEncoding == CONTENT_ENCODING) {
            "Unsupported content encoding: ${serialized.contentEncoding}. Expected: $CONTENT_ENCODING"
        }

        val jsonString = serialized.data.toString(Charsets.UTF_8)
        val eventKType = eventClass.kotlin.createType()
        val eventSerializer = serializer(eventKType)

        @Suppress("UNCHECKED_CAST")
        val typedEventSerializer = eventSerializer as KSerializer<T>
        val envelopeSerializer = EventEnvelopeSerializer(typedEventSerializer)

        return json.decodeFromString(envelopeSerializer, jsonString)
    }

    companion object {
        const val CONTENT_TYPE = "application/json"
        const val CONTENT_ENCODING = "utf-8"
    }
}
