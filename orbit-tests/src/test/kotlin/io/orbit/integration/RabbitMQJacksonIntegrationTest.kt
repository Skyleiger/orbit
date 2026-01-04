package io.orbit.integration

import io.kotest.core.extensions.install
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.orbit.core.serializer.SerializerFactory
import io.orbit.core.transport.TransportFactory
import io.orbit.serialization.jackson.JacksonSerializerFactory
import io.orbit.transport.rabbitmq.RabbitMQTransportConfig
import io.orbit.transport.rabbitmq.RabbitMQTransportFactory
import org.testcontainers.rabbitmq.RabbitMQContainer
import kotlin.uuid.Uuid

class RabbitMQJacksonIntegrationTest : IntegrationTestContract() {
    private val container = install(TestContainerSpecExtension(RabbitMQContainer("rabbitmq:4-management-alpine")))

    override fun createTransportFactory(): TransportFactory =
        RabbitMQTransportFactory(
            RabbitMQTransportConfig(
                host = container.host,
                port = container.amqpPort,
                username = container.adminUsername,
                password = container.adminPassword,
                exchangeName = generateExchangeName(),
            ),
        )

    override fun createSerializerFactory(): SerializerFactory = JacksonSerializerFactory()

    private fun generateExchangeName(): String = "orbit.${Uuid.random()}.events"

    override val eventuallyTimeoutMs: Long = 5000L
}
