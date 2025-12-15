package io.orbit.transport.inmemory

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.orbit.core.transport.TransportMessage
import kotlinx.coroutines.delay

/**
 * Tests for basic InMemoryTransport functionality.
 */
class InMemoryTransportBasicTest :
    FunSpec({
        context("Connection Management") {
            test("should start disconnected") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())

                transport.isConnected() shouldBe false
            }

            test("should connect successfully") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())

                transport.connect()

                transport.isConnected() shouldBe true
            }

            test("should disconnect successfully") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())

                transport.connect()
                transport.disconnect()

                transport.isConnected() shouldBe false
            }

            test("should allow reconnection after disconnect") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())

                transport.connect()
                transport.disconnect()
                transport.connect()

                transport.isConnected() shouldBe true
            }
        }

        context("Basic Message Delivery") {
            test("should send and receive message") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())
                val eventType = TestFactory.createEventType()
                val message = TestFactory.createMessage()

                var receivedMessage: TransportMessage? = null
                transport.connectAndSubscribe(eventType, TestFactory.createHandler { receivedMessage = it })

                transport.sendAndWait(message)

                receivedMessage shouldBe message
            }

            test("should not deliver message to unsubscribed event type") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())
                val subscribedEvent = TestFactory.createEventType("subscribed.event")
                val unsubscribedEvent = TestFactory.createEventType("unsubscribed.event")
                val message = TestFactory.createMessage(eventType = unsubscribedEvent.value)

                var receivedMessage: TransportMessage? = null
                transport.connectAndSubscribe(subscribedEvent, TestFactory.createHandler { receivedMessage = it })

                transport.sendAndWait(message)

                receivedMessage shouldBe null
            }

            test("should handle multiple event types independently") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())
                val eventType1 = TestFactory.createEventType("event.type1")
                val eventType2 = TestFactory.createEventType("event.type2")
                val message1 = TestFactory.createMessage(id = "msg-1", eventType = eventType1.value, body = "message1")
                val message2 = TestFactory.createMessage(id = "msg-2", eventType = eventType2.value, body = "message2")

                var receivedMessage1: TransportMessage? = null
                var receivedMessage2: TransportMessage? = null

                transport.connect()
                transport.subscribe(eventType1, TestFactory.createHandler { receivedMessage1 = it })
                transport.subscribe(eventType2, TestFactory.createHandler { receivedMessage2 = it })

                transport.send(message1)
                transport.send(message2)
                delay(TestConstants.TEST_DELAY_MS)

                receivedMessage1 shouldBe message1
                receivedMessage2 shouldBe message2
            }
        }

        context("Subscription Management") {
            test("should unsubscribe from event type") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())
                val eventType = TestFactory.createEventType()
                val message = TestFactory.createMessage()

                var receivedMessage: TransportMessage? = null
                transport.connectAndSubscribe(eventType, TestFactory.createHandler { receivedMessage = it })
                transport.unsubscribe(eventType)

                transport.sendAndWait(message)

                receivedMessage shouldBe null
            }

            test("should cleanup all subscriptions on disconnect") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())
                val eventType = TestFactory.createEventType()
                val message = TestFactory.createMessage()

                var receivedMessage: TransportMessage? = null
                transport.connectAndSubscribe(eventType, TestFactory.createHandler { receivedMessage = it })
                transport.disconnect()

                // Try to send after disconnect (should not throw, but message won't be delivered)
                transport.connect()
                transport.sendAndWait(message)

                receivedMessage shouldBe null
            }

            test("should handle multiple subscriptions on same transport") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())
                val event1 = TestFactory.createEventType("event1")
                val event2 = TestFactory.createEventType("event2")
                val event3 = TestFactory.createEventType("event3")

                var count1 = 0
                var count2 = 0
                var count3 = 0

                transport.connect()
                transport.subscribe(event1, TestFactory.createHandler { count1++ })
                transport.subscribe(event2, TestFactory.createHandler { count2++ })
                transport.subscribe(event3, TestFactory.createHandler { count3++ })

                transport.send(TestFactory.createMessage(eventType = event1.value))
                transport.send(TestFactory.createMessage(eventType = event2.value))
                transport.send(TestFactory.createMessage(eventType = event3.value))
                delay(TestConstants.TEST_DELAY_MS)

                count1 shouldBe 1
                count2 shouldBe 1
                count3 shouldBe 1
            }
        }
    })
