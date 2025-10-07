package io.orbit.core.event

import kotlin.time.Instant

data class EventMetadata(
    val eventId: String,
    val eventType: String,
    val timestamp: Instant,
    val source: String,
    val tenant: String? = null,
    val correlationId: String? = null,
    val headers: Map<String, String> = emptyMap(),
)
