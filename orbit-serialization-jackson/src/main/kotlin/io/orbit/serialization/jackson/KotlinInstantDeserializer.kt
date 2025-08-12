package io.orbit.serialization.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class KotlinInstantDeserializer : JsonDeserializer<Instant>() {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Instant {
    return Instant.parse(p.text)
  }
}