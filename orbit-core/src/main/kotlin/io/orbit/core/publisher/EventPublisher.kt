package io.orbit.core.publisher

import kotlin.time.Duration

interface EventPublisher {

  suspend fun publish(event: Any)

  suspend fun publishTo(serviceName: String, event: Any)

  suspend fun publishDelayed(event: Any, delay: Duration)

  suspend fun publishDelayedTo(serviceName: String, event: Any, delay: Duration)

}