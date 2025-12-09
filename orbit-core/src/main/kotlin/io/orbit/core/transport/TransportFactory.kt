package io.orbit.core.transport

import io.orbit.core.service.ServiceIdentity

/**
 * Factory interface for creating [MessageTransport] instances.
 *
 * This allows the [io.orbit.core.OrbitBuilder] to defer transport creation until
 * all necessary dependencies are available.
 */
fun interface TransportFactory {
    /**
     * Creates a new [MessageTransport] instance.
     *
     * @param serviceIdentity The identity of the service that will use this transport.
     * @return A new transport instance configured for the given service.
     */
    fun create(serviceIdentity: ServiceIdentity): MessageTransport
}
