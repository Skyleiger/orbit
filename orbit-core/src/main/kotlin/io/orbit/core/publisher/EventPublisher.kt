package io.orbit.core.publisher

import io.orbit.core.event.EventEnvelope
import io.orbit.core.event.EventMetadata
import io.orbit.core.event.EventRegistry
import io.orbit.core.serializer.EventSerializer
import io.orbit.core.service.ServiceIdentity
import io.orbit.core.transport.MessageTransport
import io.orbit.core.transport.TransportMessage
import java.util.UUID
import kotlin.time.TimeSource

interface EventPublisher {
    suspend fun publish(event: Any)
}

internal class DefaultEventPublisher(
    private val eventRegistry: EventRegistry,
    private val serializer: EventSerializer,
    private val transport: MessageTransport,
    private val service: ServiceIdentity,
) : EventPublisher {
    override suspend fun publish(event: Any) {
        val eventDefinition =
            eventRegistry.findEvent(event::class)
                ?: error("Event ${event::class.simpleName} is not registered")

        val metadata =
            EventMetadata(
                eventId = UUID.randomUUID().toString(),
                eventType = eventDefinition.eventType.value,
                timestamp = TimeSource.Monotonic.markNow().elapsedNow(),
                source = service.source,
            )

        val envelope = EventEnvelope(event, metadata)

        val serialized = serializer.serialize(envelope)

        val transportMessage =
            TransportMessage(
                messageId = metadata.eventId,
                eventType = metadata.eventType,
                contentType = serialized.contentType,
                contentEncoding = serialized.contentEncoding,
                body = serialized.data,
            )

        transport.send(transportMessage)
    }
}
