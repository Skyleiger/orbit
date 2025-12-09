package io.orbit.transport.rabbitmq

import io.orbit.core.service.ServiceIdentity
import io.orbit.core.transport.TransportFactory

/**
 * Factory for creating [RabbitMQTransport] instances.
 *
 * @param config The RabbitMQ connection and messaging configuration.
 */
class RabbitMQTransportFactory(
    private val config: RabbitMQTransportConfig,
) : TransportFactory {
    override fun create(serviceIdentity: ServiceIdentity) = RabbitMQTransport(config, serviceIdentity)
}
