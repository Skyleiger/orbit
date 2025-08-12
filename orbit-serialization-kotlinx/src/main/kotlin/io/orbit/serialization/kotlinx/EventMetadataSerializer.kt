package io.orbit.serialization.kotlinx

import io.orbit.core.event.EventMetadata
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(ExperimentalSerializationApi::class, ExperimentalTime::class)
object EventMetadataSerializer : KSerializer<EventMetadata> {
  override val descriptor: SerialDescriptor = buildClassSerialDescriptor("EventMetadata") {
    element<String>("eventId")
    element<String>("eventType")
    element<String>("timestamp")
    element<String>("source")
    element<String?>("tenant")
    element<String?>("correlationId")
    element<Map<String, String>>("headers")
  }

  override fun serialize(encoder: Encoder, value: EventMetadata) {
    val composite = encoder.beginStructure(descriptor)
    composite.encodeStringElement(descriptor, 0, value.eventId)
    composite.encodeStringElement(descriptor, 1, value.eventType)
    composite.encodeStringElement(descriptor, 2, value.timestamp.toString())
    composite.encodeStringElement(descriptor, 3, value.source)
    composite.encodeNullableSerializableElement(descriptor, 4, String.serializer().nullable, value.tenant)
    composite.encodeNullableSerializableElement(descriptor, 5, String.serializer().nullable, value.correlationId)
    composite.encodeSerializableElement(descriptor, 6, MapSerializer(String.serializer(), String.serializer()), value.headers)
    composite.endStructure(descriptor)
  }

  override fun deserialize(decoder: Decoder): EventMetadata {
    val dec = decoder.beginStructure(descriptor)
    var eventId = ""
    var eventType = ""
    var timestamp: Instant = Instant.DISTANT_PAST
    var source = ""
    var tenant: String? = null
    var correlationId: String? = null
    var headers: Map<String, String> = emptyMap()

    loop@ while (true) {
      when (val index = dec.decodeElementIndex(descriptor)) {
        CompositeDecoder.DECODE_DONE -> break@loop
        0 -> eventId = dec.decodeStringElement(descriptor, 0)
        1 -> eventType = dec.decodeStringElement(descriptor, 1)
        2 -> timestamp = Instant.parse(dec.decodeStringElement(descriptor, 2))
        3 -> source = dec.decodeStringElement(descriptor, 3)
        4 -> tenant = dec.decodeNullableSerializableElement(descriptor, 4, String.serializer().nullable)
        5 -> correlationId = dec.decodeNullableSerializableElement(descriptor, 5, String.serializer().nullable)
        6 -> headers = dec.decodeSerializableElement(descriptor, 6, MapSerializer(String.serializer(), String.serializer()))
        else -> throw IllegalArgumentException("Unknown index $index")
      }
    }
    dec.endStructure(descriptor)

    return EventMetadata(
      eventId = eventId,
      eventType = eventType,
      timestamp = timestamp,
      source = source,
      tenant = tenant,
      correlationId = correlationId,
      headers = headers
    )
  }
}