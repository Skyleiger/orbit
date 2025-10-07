package io.orbit.transport.inmemory

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.orbit.core.transport.MessageHandler
import io.orbit.core.transport.TransportMessage

class InMemoryTransportTest :
    StringSpec({
        "should send message to subscribed handler" {
            val transport = InMemoryTransport()
            val destination = "test-destination"
            val message =
                TransportMessage(
                    routingKey = "test.route",
                    payload = "test message".toByteArray(),
                )
            var receivedMessage: TransportMessage? = null

            val handler = MessageHandler { receivedMessage = it }
            transport.subscribe(destination, handler)

            transport.send(destination, message)

            receivedMessage shouldBe message
        }

        "should send message to multiple handlers for same destination" {
            val transport = InMemoryTransport()
            val destination = "test-destination"
            val message =
                TransportMessage(
                    routingKey = "test.route",
                    payload = "test message".toByteArray(),
                )
            val receivedMessages = mutableListOf<TransportMessage>()

            val handler1 = MessageHandler { receivedMessages.add(it) }
            val handler2 = MessageHandler { receivedMessages.add(it) }

            transport.subscribe(destination, handler1)
            transport.subscribe(destination, handler2)

            transport.send(destination, message)

            receivedMessages shouldHaveSize 2
            receivedMessages.forEach { it shouldBe message }
        }

        "should not send message when no handlers are subscribed" {
            val transport = InMemoryTransport()
            val destination = "test-destination"
            val message =
                TransportMessage(
                    routingKey = "test.route",
                    payload = "test message".toByteArray(),
                )

            transport.send(destination, message)
        }

        "should handle handler exceptions gracefully" {
            val transport = InMemoryTransport()
            val destination = "test-destination"
            val message =
                TransportMessage(
                    routingKey = "test.route",
                    payload = "test message".toByteArray(),
                )
            var handlerCalled = false

            val throwingHandler = MessageHandler { throw RuntimeException("Handler error") }
            val normalHandler = MessageHandler { handlerCalled = true }

            transport.subscribe(destination, throwingHandler)
            transport.subscribe(destination, normalHandler)

            transport.send(destination, message)

            handlerCalled shouldBe true
        }

        "should send message with headers" {
            val transport = InMemoryTransport()
            val destination = "test-destination"
            val headers = mapOf("header1" to "value1", "header2" to "value2")
            val message =
                TransportMessage(
                    routingKey = "test.route",
                    payload = "test message".toByteArray(),
                    headers = headers,
                )
            var receivedMessage: TransportMessage? = null

            val handler = MessageHandler { receivedMessage = it }
            transport.subscribe(destination, handler)

            transport.send(destination, message)

            receivedMessage?.headers shouldBe headers
        }

        "should handle multiple destinations independently" {
            val transport = InMemoryTransport()
            val destination1 = "destination1"
            val destination2 = "destination2"
            val message1 = TransportMessage(routingKey = "route1", payload = "message1".toByteArray())
            val message2 = TransportMessage(routingKey = "route2", payload = "message2".toByteArray())

            var receivedMessage1: TransportMessage? = null
            var receivedMessage2: TransportMessage? = null

            transport.subscribe(destination1) { receivedMessage1 = it }
            transport.subscribe(destination2) { receivedMessage2 = it }

            transport.send(destination1, message1)
            transport.send(destination2, message2)

            receivedMessage1 shouldBe message1
            receivedMessage2 shouldBe message2
        }
    })
