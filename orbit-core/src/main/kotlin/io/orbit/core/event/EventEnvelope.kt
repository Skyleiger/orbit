package io.orbit.core.event

data class EventEnvelope<T : Any>(
  val event: T,
  val metadata: EventMetadata
)