package io.orbit.transport.rabbitmq

import io.kotest.core.extensions.install
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.orbit.core.transport.MessageTransportTestContract
import io.orbit.core.transport.TransportFactory
import org.testcontainers.rabbitmq.RabbitMQContainer
import kotlin.uuid.Uuid

class RabbitMQTransportTest : MessageTransportTestContract() {
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

    private fun generateExchangeName(): String = "orbit.${Uuid.random()}.events"

    override val eventuallyTimeoutMs: Long = 5000L
}
