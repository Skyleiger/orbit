package io.orbit.core.event

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Event(
    val type: String,
)
