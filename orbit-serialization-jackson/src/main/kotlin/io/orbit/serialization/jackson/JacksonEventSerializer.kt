package io.orbit.serialization.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.orbit.core.event.EventEnvelope
import io.orbit.core.serializer.EventSerializer
import io.orbit.core.serializer.SerializedEvent
import kotlin.time.Instant

class JacksonEventSerializer(
    private val objectMapper: ObjectMapper = createDefaultObjectMapper(),
) : EventSerializer {
    override fun <T : Any> serialize(envelope: EventEnvelope<T>): SerializedEvent =
        SerializedEvent(
            data = objectMapper.writeValueAsBytes(envelope),
            contentType = CONTENT_TYPE,
            contentEncoding = CONTENT_ENCODING,
        )

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

        val envelopeType = objectMapper.typeFactory.constructParametricType(EventEnvelope::class.java, eventClass)
        return objectMapper.readValue(serialized.data, envelopeType)
    }

    companion object {
        const val CONTENT_TYPE = "application/json"
        const val CONTENT_ENCODING = "utf-8"

        private fun createDefaultObjectMapper(): ObjectMapper {
            val module =
                SimpleModule().apply {
                    addSerializer(Instant::class.java, KotlinInstantSerializer())
                    addDeserializer(Instant::class.java, KotlinInstantDeserializer())
                }

            return jacksonObjectMapper().apply {
                registerModule(module)
            }
        }
    }
}
