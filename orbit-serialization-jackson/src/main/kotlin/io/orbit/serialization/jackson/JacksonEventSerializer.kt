package io.orbit.serialization.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.orbit.core.event.EventEnvelope
import io.orbit.core.serializer.EventSerializer
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class JacksonEventSerializer(
  private val objectMapper: ObjectMapper = createDefaultObjectMapper()
) : EventSerializer {

  override fun <T : Any> serialize(envelope: EventEnvelope<T>): ByteArray {
    return objectMapper.writeValueAsBytes(envelope)
  }

  override fun <T : Any> deserialize(data: ByteArray, eventType: Class<T>): EventEnvelope<T> {
    val envelopeType = objectMapper.typeFactory.constructParametricType(EventEnvelope::class.java, eventType)
    return objectMapper.readValue(data, envelopeType)
  }

  companion object {
    private fun createDefaultObjectMapper(): ObjectMapper {
      val module = SimpleModule().apply {
        addSerializer(Instant::class.java, KotlinInstantSerializer())
        addDeserializer(Instant::class.java, KotlinInstantDeserializer())
      }
      
      return jacksonObjectMapper().apply {
        registerModule(module)
      }
    }
  }
}