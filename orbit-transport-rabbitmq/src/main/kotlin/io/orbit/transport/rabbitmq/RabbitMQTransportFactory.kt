package io.orbit.transport.rabbitmq

import io.orbit.core.service.ServiceIdentity
import io.orbit.core.transport.TransportFactory

/**
 * Factory for creating [RabbitMQTransport] instances.
 */
class RabbitMQTransportFactory : TransportFactory {
    override fun create(serviceIdentity: ServiceIdentity) = RabbitMQTransport()
}
