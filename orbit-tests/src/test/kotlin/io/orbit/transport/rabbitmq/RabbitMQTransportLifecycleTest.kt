package io.orbit.transport.rabbitmq

import io.kotest.core.extensions.install
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.matchers.shouldBe
import io.orbit.core.event.EventType
import io.orbit.core.service.ServiceIdentity
import io.orbit.core.transport.TransportMessage
import kotlinx.coroutines.delay
import org.testcontainers.rabbitmq.RabbitMQContainer

/**
 * Tests for RabbitMQ transport reconnect scenarios.
 *
 * These tests verify that the transport can be disconnected and reconnected
 * multiple times, which is important for:
 * - Connection recovery after network failures
 * - Graceful shutdown and restart scenarios
 * - Resource cleanup verification
 */
class RabbitMQTransportLifecycleTest :
    StringSpec({
        val rabbit = install(TestContainerSpecExtension(RabbitMQContainer("rabbitmq:4-management-alpine")))

        fun createConfig() =
            RabbitMQTransportConfig(
                host = rabbit.host,
                port = rabbit.amqpPort,
            )

        "should connect and disconnect" {
            val transport =
                RabbitMQTransport(
                    config = createConfig(),
                    serviceIdentity = ServiceIdentity("test-service"),
                )

            transport.isConnected() shouldBe false

            transport.connect()
            transport.isConnected() shouldBe true

            transport.disconnect()
            transport.isConnected() shouldBe false
        }

        "should support multiple reconnect cycles" {
            val transport =
                RabbitMQTransport(
                    config = createConfig(),
                    serviceIdentity = ServiceIdentity("reconnect-test"),
                )

            repeat(3) { _ ->
                transport.connect()
                transport.isConnected() shouldBe true

                transport.disconnect()
                transport.isConnected() shouldBe false

                delay(50)
            }
        }

        "should handle messages after reconnect with re-registered handlers" {
            val publisher =
                RabbitMQTransport(
                    config = createConfig(),
                    serviceIdentity = ServiceIdentity("publisher-reconnect"),
                )
            val subscriber =
                RabbitMQTransport(
                    config = createConfig(),
                    serviceIdentity = ServiceIdentity("subscriber-reconnect"),
                )

            val eventType = EventType("reconnect.test")
            var messageCount = 0

            // First lifecycle: connect, subscribe, send, receive
            publisher.connect()
            subscriber.connect()
            subscriber.subscribe(eventType) { messageCount++ }
            delay(100)

            publisher.send(
                TransportMessage(
                    messageId = "msg-1",
                    eventType = eventType.value,
                    contentType = "application/json",
                    contentEncoding = "utf-8",
                    body = "test1".toByteArray(),
                ),
            )
            delay(500)
            messageCount shouldBe 1

            // Disconnect (handlers are cleared)
            subscriber.disconnect()
            delay(100)

            // Second lifecycle: reconnect and re-subscribe (Orbit-core's responsibility)
            subscriber.connect()
            subscriber.subscribe(eventType) { messageCount++ }
            delay(100)

            publisher.send(
                TransportMessage(
                    messageId = "msg-2",
                    eventType = eventType.value,
                    contentType = "application/json",
                    contentEncoding = "utf-8",
                    body = "test2".toByteArray(),
                ),
            )
            delay(500)
            messageCount shouldBe 2

            publisher.disconnect()
            subscriber.disconnect()
        }
    })
