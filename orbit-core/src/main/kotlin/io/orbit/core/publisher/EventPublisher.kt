package io.orbit.core.publisher

interface EventPublisher {
    suspend fun publish(event: Any)
}

internal class DefaultEventPublisher : EventPublisher {
    override suspend fun publish(event: Any) {
        TODO("Not yet implemented")
    }
}
