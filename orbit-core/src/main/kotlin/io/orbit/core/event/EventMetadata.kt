package io.orbit.core.event

import kotlin.time.Instant

data class EventMetadata(
    val eventId: String,
    val eventType: String,
    val timestamp: Instant,
    val source: String,
)
