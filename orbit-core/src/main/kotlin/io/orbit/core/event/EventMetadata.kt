package io.orbit.core.event

import kotlin.time.Duration

data class EventMetadata(
    val eventId: String,
    val eventType: String,
    val timestamp: Duration,
    val source: String,
)
