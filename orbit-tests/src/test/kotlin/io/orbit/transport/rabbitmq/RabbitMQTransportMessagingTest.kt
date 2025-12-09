package io.orbit.transport.rabbitmq

import io.kotest.core.extensions.install
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.orbit.core.event.EventType
import io.orbit.core.service.ServiceIdentity
import io.orbit.core.transport.MessageHandler
import io.orbit.core.transport.TransportMessage
import kotlinx.coroutines.delay
import org.testcontainers.rabbitmq.RabbitMQContainer

/**
 * Integration tests for RabbitMQ transport messaging functionality.
 *
 * Tests cover:
 * - Publishing and subscribing to events
 * - Multiple subscribers and handlers
 * - Event type filtering
 * - Unsubscribe operations
 */
class RabbitMQTransportMessagingTest :
    StringSpec({
        val rabbit = install(TestContainerSpecExtension(RabbitMQContainer("rabbitmq:4-management-alpine")))

        fun createConfig() =
            RabbitMQTransportConfig(
                host = rabbit.host,
                port = rabbit.amqpPort,
            )

        "should send message to subscribed handler" {
            val serviceA =
                RabbitMQTransport(
                    config = createConfig(),
                    serviceIdentity = ServiceIdentity("service-a"),
                )
            val serviceB =
                RabbitMQTransport(
                    config = createConfig(),
                    serviceIdentity = ServiceIdentity("service-b"),
                )

            serviceA.connect()
            serviceB.connect()

            val eventType = EventType("user.created")
            val userIdJson = "{\"userId\": 1}"
            val message =
                TransportMessage(
                    messageId = "msg-1",
                    eventType = eventType.value,
                    contentType = "application/json",
                    contentEncoding = "utf-8",
                    body = userIdJson.toByteArray(),
                )
            var receivedMessage: TransportMessage? = null

            val handler = MessageHandler { receivedMessage = it }
            serviceB.subscribe(eventType, handler)

            delay(100)

            serviceA.send(message)
            delay(500)

            receivedMessage?.messageId shouldBe message.messageId
            receivedMessage?.eventType shouldBe message.eventType
            receivedMessage?.body?.decodeToString() shouldBe message.body.decodeToString()

            serviceA.disconnect()
            serviceB.disconnect()
        }

        "should send message to multiple subscribers" {
            val publisher =
                RabbitMQTransport(
                    config = createConfig(),
                    serviceIdentity = ServiceIdentity("publisher"),
                )
            val subscriber1 =
                RabbitMQTransport(
                    config = createConfig(),
                    serviceIdentity = ServiceIdentity("subscriber-1"),
                )
            val subscriber2 =
                RabbitMQTransport(
                    config = createConfig(),
                    serviceIdentity = ServiceIdentity("subscriber-2"),
                )

            publisher.connect()
            subscriber1.connect()
            subscriber2.connect()

            val eventType = EventType("order.placed")
            val orderJson = "{\"orderId\": 42}"
            val message =
                TransportMessage(
                    messageId = "msg-multi",
                    eventType = eventType.value,
                    contentType = "application/json",
                    contentEncoding = "utf-8",
                    body = orderJson.toByteArray(),
                )

            val receivedMessages = mutableListOf<TransportMessage>()
            val handler = MessageHandler { receivedMessages.add(it) }

            subscriber1.subscribe(eventType, handler)
            subscriber2.subscribe(eventType, handler)

            delay(100)

            publisher.send(message)
            delay(500)

            receivedMessages shouldHaveSize 2
            receivedMessages.forEach { it.eventType shouldBe eventType.value }

            publisher.disconnect()
            subscriber1.disconnect()
            subscriber2.disconnect()
        }

        "should not receive unbound event types" {
            val publisher =
                RabbitMQTransport(
                    config = createConfig(),
                    serviceIdentity = ServiceIdentity("publisher-unbound"),
                )
            val subscriber =
                RabbitMQTransport(
                    config = createConfig(),
                    serviceIdentity = ServiceIdentity("subscriber-unbound"),
                )

            publisher.connect()
            subscriber.connect()

            var receivedMessage: TransportMessage? = null
            subscriber.subscribe(EventType("event.subscribed")) { receivedMessage = it }

            delay(100)

            publisher.send(
                TransportMessage(
                    messageId = "msg-unbound",
                    eventType = "event.not-subscribed",
                    contentType = "application/json",
                    contentEncoding = "utf-8",
                    body = "test".toByteArray(),
                ),
            )
            delay(300)

            receivedMessage shouldBe null

            publisher.disconnect()
            subscriber.disconnect()
        }

        "should unsubscribe correctly" {
            val publisher =
                RabbitMQTransport(
                    config = createConfig(),
                    serviceIdentity = ServiceIdentity("publisher-unsub"),
                )
            val subscriber =
                RabbitMQTransport(
                    config = createConfig(),
                    serviceIdentity = ServiceIdentity("subscriber-unsub"),
                )

            publisher.connect()
            subscriber.connect()

            val eventType = EventType("event.to-unsubscribe")
            var receivedCount = 0
            subscriber.subscribe(eventType) { receivedCount++ }

            delay(100)

            publisher.send(
                TransportMessage(
                    messageId = "msg-before-unsub",
                    eventType = eventType.value,
                    contentType = "application/json",
                    contentEncoding = "utf-8",
                    body = "test".toByteArray(),
                ),
            )
            delay(300)

            receivedCount shouldBe 1

            subscriber.unsubscribe(eventType)
            delay(100)

            publisher.send(
                TransportMessage(
                    messageId = "msg-after-unsub",
                    eventType = eventType.value,
                    contentType = "application/json",
                    contentEncoding = "utf-8",
                    body = "test".toByteArray(),
                ),
            )
            delay(300)

            receivedCount shouldBe 1

            publisher.disconnect()
            subscriber.disconnect()
        }

        "should handle multiple handlers for same event type" {
            val publisher =
                RabbitMQTransport(
                    config = createConfig(),
                    serviceIdentity = ServiceIdentity("publisher-multi-handler"),
                )
            val subscriber =
                RabbitMQTransport(
                    config = createConfig(),
                    serviceIdentity = ServiceIdentity("subscriber-multi-handler"),
                )

            publisher.connect()
            subscriber.connect()

            val eventType = EventType("event.multi-handler")
            val receivedMessages = mutableListOf<String>()

            subscriber.subscribe(eventType) { receivedMessages.add("handler1") }
            subscriber.subscribe(eventType) { receivedMessages.add("handler2") }

            delay(100)

            publisher.send(
                TransportMessage(
                    messageId = "msg-multi-handler",
                    eventType = eventType.value,
                    contentType = "application/json",
                    contentEncoding = "utf-8",
                    body = "test".toByteArray(),
                ),
            )
            delay(300)

            receivedMessages shouldHaveSize 2
            receivedMessages shouldBe listOf("handler1", "handler2")

            publisher.disconnect()
            subscriber.disconnect()
        }
    })
