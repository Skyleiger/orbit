package io.orbit.serialization.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class KotlinInstantSerializer : JsonSerializer<Instant>() {
  override fun serialize(value: Instant, gen: JsonGenerator, serializers: SerializerProvider) {
    gen.writeString(value.toString())
  }
}