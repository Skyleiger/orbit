package io.orbit.serialization.kotlinx

import io.orbit.core.event.EventEnvelope
import io.orbit.core.event.EventMetadata
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class EventEnvelopeSerializer<T : Any>(
  private val dataSerializer: KSerializer<T>
) : KSerializer<EventEnvelope<T>> {
  override val descriptor: SerialDescriptor = buildClassSerialDescriptor("EventEnvelope") {
    element("event", dataSerializer.descriptor)
    element("metadata", EventMetadataSerializer.descriptor)
  }

  override fun serialize(encoder: Encoder, value: EventEnvelope<T>) {
    val composite = encoder.beginStructure(descriptor)
    composite.encodeSerializableElement(descriptor, 0, dataSerializer, value.event)
    composite.encodeSerializableElement(descriptor, 1, EventMetadataSerializer, value.metadata)
    composite.endStructure(descriptor)
  }

  override fun deserialize(decoder: Decoder): EventEnvelope<T> {
    val dec = decoder.beginStructure(descriptor)
    var event: T? = null
    var metadata: EventMetadata? = null

    loop@ while (true) {
      when (val index = dec.decodeElementIndex(descriptor)) {
        CompositeDecoder.DECODE_DONE -> break@loop
        0 -> event = dec.decodeSerializableElement(descriptor, 0, dataSerializer)
        1 -> metadata = dec.decodeSerializableElement(descriptor, 1, EventMetadataSerializer)
        else -> throw IllegalArgumentException("Unknown index $index")
      }
    }
    dec.endStructure(descriptor)

    return EventEnvelope(
      event = event ?: throw IllegalStateException("Missing event in EventEnvelope"),
      metadata = metadata ?: throw IllegalStateException("Missing metadata in EventEnvelope")
    )
  }
}