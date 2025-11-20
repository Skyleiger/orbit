package io.orbit.transport.inmemory

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.orbit.core.transport.MessageHandler
import io.orbit.core.transport.TransportMessage
import kotlinx.coroutines.delay

class InMemoryTransportTest :
    StringSpec({
        "should send message to subscribed handler" {
            val transport = InMemoryTransport()
            transport.connect()
            val eventType = "test.event"
            val message =
                TransportMessage(
                    messageId = "msg-1",
                    eventType = eventType,
                    contentType = "application/json",
                    contentEncoding = "utf-8",
                    body = "test message".toByteArray(),
                )
            var receivedMessage: TransportMessage? = null

            val handler = MessageHandler { receivedMessage = it }
            transport.subscribe(eventType, handler)

            transport.send(message)
            delay(10)

            receivedMessage shouldBe message
        }

        "should send message to multiple handlers for same eventType" {
            val transport = InMemoryTransport()
            transport.connect()
            val eventType = "test.event"
            val message =
                TransportMessage(
                    messageId = "msg-1",
                    eventType = eventType,
                    contentType = "application/json",
                    contentEncoding = "utf-8",
                    body = "test message".toByteArray(),
                )
            val receivedMessages = mutableListOf<TransportMessage>()

            val handler1 = MessageHandler { receivedMessages.add(it) }
            val handler2 = MessageHandler { receivedMessages.add(it) }

            transport.subscribe(eventType, handler1)
            transport.subscribe(eventType, handler2)

            transport.send(message)
            delay(10)

            receivedMessages shouldHaveSize 2
            receivedMessages.forEach { it shouldBe message }
        }

        "should not send message when no handlers are subscribed" {
            val transport = InMemoryTransport()
            transport.connect()
            val message =
                TransportMessage(
                    messageId = "msg-1",
                    eventType = "unsubscribed.event",
                    contentType = "application/json",
                    contentEncoding = "utf-8",
                    body = "test message".toByteArray(),
                )

            transport.send(message)
            delay(10)
        }

        "should handle multiple event types independently" {
            val transport = InMemoryTransport()
            transport.connect()
            val eventType1 = "event.type1"
            val eventType2 = "event.type2"
            val message1 =
                TransportMessage(
                    messageId = "msg-1",
                    eventType = eventType1,
                    contentType = "application/json",
                    contentEncoding = "utf-8",
                    body = "message1".toByteArray(),
                )
            val message2 =
                TransportMessage(
                    messageId = "msg-2",
                    eventType = eventType2,
                    contentType = "application/json",
                    contentEncoding = "utf-8",
                    body = "message2".toByteArray(),
                )

            var receivedMessage1: TransportMessage? = null
            var receivedMessage2: TransportMessage? = null

            transport.subscribe(eventType1) { receivedMessage1 = it }
            transport.subscribe(eventType2) { receivedMessage2 = it }

            transport.send(message1)
            transport.send(message2)
            delay(10)

            receivedMessage1 shouldBe message1
            receivedMessage2 shouldBe message2
        }

        "should unsubscribe event type" {
            val transport = InMemoryTransport()
            transport.connect()
            val eventType = "test.event"
            val message =
                TransportMessage(
                    messageId = "msg-1",
                    eventType = eventType,
                    contentType = "application/json",
                    contentEncoding = "utf-8",
                    body = "test message".toByteArray(),
                )
            var receivedMessage: TransportMessage? = null

            transport.subscribe(eventType) { receivedMessage = it }
            transport.unsubscribe(eventType)

            transport.send(message)
            delay(10)

            receivedMessage shouldBe null
        }

        "should connect and disconnect" {
            val transport = InMemoryTransport()

            transport.isConnected() shouldBe false

            transport.connect()
            transport.isConnected() shouldBe true

            transport.disconnect()
            transport.isConnected() shouldBe false
        }
    })
