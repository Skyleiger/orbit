package io.orbit.core.handler

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventHandler(
  val eventType: KClass<Any>,
  val deadLetterQueue: Boolean = true,
  val maxRetries: Int = 3,
  val retryDelay: Long = 5000 // ms
)