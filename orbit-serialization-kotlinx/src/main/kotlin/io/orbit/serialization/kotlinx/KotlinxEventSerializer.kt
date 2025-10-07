package io.orbit.serialization.kotlinx

import io.orbit.core.event.EventEnvelope
import io.orbit.core.serializer.EventSerializer
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
    override fun <T : Any> serialize(envelope: EventEnvelope<T>): ByteArray {
        val eventClass = envelope.event::class
        val eventSerializer = serializer(eventClass.createType())

        @Suppress("UNCHECKED_CAST")
        val typedEventSerializer = eventSerializer as KSerializer<T>
        val envelopeSerializer = EventEnvelopeSerializer(typedEventSerializer)

        val jsonString = json.encodeToString(envelopeSerializer, envelope)
        return jsonString.toByteArray(Charsets.UTF_8)
    }

    override fun <T : Any> deserialize(
        data: ByteArray,
        eventType: Class<T>,
    ): EventEnvelope<T> {
        val jsonString = data.toString(Charsets.UTF_8)
        val eventKType = eventType.kotlin.createType()
        val eventSerializer = serializer(eventKType)

        @Suppress("UNCHECKED_CAST")
        val typedEventSerializer = eventSerializer as KSerializer<T>
        val envelopeSerializer = EventEnvelopeSerializer(typedEventSerializer)

        return json.decodeFromString(envelopeSerializer, jsonString)
    }
}
