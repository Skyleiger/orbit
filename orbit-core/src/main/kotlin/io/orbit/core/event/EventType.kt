package io.orbit.core.event

@JvmInline
value class EventType(
    val value: String,
) {
    init {
        require(value.isNotEmpty()) { "EventType cannot be empty" }
        require(value.isNotBlank()) { "EventType cannot be blank" }
    }
}
