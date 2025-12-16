package io.orbit.transport.inmemory

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain

/**
 * Tests for error handling and edge cases.
 */
class InMemoryTransportErrorHandlingTest :
    FunSpec({
        context("Error Handling - Operations Without Connection") {
            test("should throw when sending without connection") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())
                val message = TestFactory.createMessage()

                val exception =
                    shouldThrow<IllegalStateException> {
                        transport.send(message)
                    }
                exception.message shouldContain "not connected"
            }

            test("should throw when subscribing without connection") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())
                val eventType = TestFactory.createEventType()

                val exception =
                    shouldThrow<IllegalStateException> {
                        transport.subscribe(eventType, TestFactory.createHandler {})
                    }
                exception.message shouldContain "not connected"
            }

            test("should throw when unsubscribing without connection") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())
                val eventType = TestFactory.createEventType()

                val exception =
                    shouldThrow<IllegalStateException> {
                        transport.unsubscribe(eventType)
                    }
                exception.message shouldContain "not connected"
            }
        }

        context("Error Handling - Duplicate Subscriptions") {
            test("should prevent duplicate subscriptions for same event type") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())
                val eventType = TestFactory.createEventType()

                transport.connect()
                transport.subscribe(eventType, TestFactory.createHandler {})

                val exception =
                    shouldThrow<IllegalArgumentException> {
                        transport.subscribe(eventType, TestFactory.createHandler {})
                    }
                exception.message shouldContain "already subscribed"
            }

            test("should allow subscription after unsubscribe") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())
                val eventType = TestFactory.createEventType()

                transport.connect()
                transport.subscribe(eventType, TestFactory.createHandler {})
                transport.unsubscribe(eventType)

                // Should not throw
                transport.subscribe(eventType, TestFactory.createHandler {})

                transport.disconnect()
            }

            test("should allow subscription after disconnect and reconnect") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())
                val eventType = TestFactory.createEventType()

                transport.connect()
                transport.subscribe(eventType, TestFactory.createHandler {})
                transport.disconnect()

                transport.connect()
                // Should not throw - disconnect cleared subscriptions
                transport.subscribe(eventType, TestFactory.createHandler {})

                transport.disconnect()
            }
        }

        context("Edge Cases") {
            test("should handle unsubscribe of non-existent subscription") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())
                val eventType = TestFactory.createEventType()

                transport.connect()

                // Should not throw
                transport.unsubscribe(eventType)

                transport.disconnect()
            }

            test("should handle multiple connect calls") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())

                transport.connect()
                transport.connect() // Should not throw or cause issues
                transport.connect()

                transport.disconnect()
            }

            test("should handle multiple disconnect calls") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())

                transport.connect()
                transport.disconnect()
                transport.disconnect() // Should not throw
                transport.disconnect()
            }

            test("should handle disconnect without connect") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())

                // Should not throw
                transport.disconnect()
            }
        }
    })
