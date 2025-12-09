package io.orbit.core.serializer

import io.orbit.core.service.ServiceIdentity

/**
 * Factory interface for creating [EventSerializer] instances.
 *
 * This allows the [io.orbit.core.OrbitBuilder] to defer serializer creation until
 * all necessary dependencies are available.
 */
fun interface SerializerFactory {
    /**
     * Creates a new [EventSerializer] instance.
     *
     * @param serviceIdentity The identity of the service that will use this serializer.
     * @return A new serializer instance configured for the given service.
     */
    fun create(serviceIdentity: ServiceIdentity): EventSerializer
}
