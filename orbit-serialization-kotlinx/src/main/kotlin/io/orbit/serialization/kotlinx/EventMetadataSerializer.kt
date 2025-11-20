package io.orbit.serialization.kotlinx

import io.orbit.core.event.EventMetadata
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

object EventMetadataSerializer : KSerializer<EventMetadata> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("EventMetadata") {
            element<String>("eventId")
            element<String>("eventType")
            element<String>("timestamp")
            element<String>("source")
        }

    override fun serialize(
        encoder: Encoder,
        value: EventMetadata,
    ) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeStringElement(descriptor, 0, value.eventId)
        composite.encodeStringElement(descriptor, 1, value.eventType)
        composite.encodeStringElement(descriptor, 2, value.timestamp.toString())
        composite.encodeStringElement(descriptor, 3, value.source)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): EventMetadata {
        val dec = decoder.beginStructure(descriptor)
        var eventId = ""
        var eventType = ""
        var timestamp = Instant.DISTANT_PAST
        var source = ""

        loop@ while (true) {
            when (val index = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> eventId = dec.decodeStringElement(descriptor, 0)
                1 -> eventType = dec.decodeStringElement(descriptor, 1)
                2 -> timestamp = Instant.parse(dec.decodeStringElement(descriptor, 2))
                3 -> source = dec.decodeStringElement(descriptor, 3)
                else -> throw IllegalArgumentException("Unknown index $index")
            }
        }
        dec.endStructure(descriptor)

        return EventMetadata(
            eventId = eventId,
            eventType = eventType,
            timestamp = timestamp,
            source = source,
        )
    }
}
