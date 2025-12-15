package io.orbit.transport.inmemory

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.orbit.core.service.ServiceIdentity
import io.orbit.core.transport.TransportMessage
import kotlinx.coroutines.delay

/**
 * Tests for load balancing behavior across service instances.
 */
class InMemoryTransportLoadBalancingTest :
    FunSpec({
        context("Load Balancing") {
            test("should distribute messages to one instance per service") {
                val factory = InMemoryTransportFactory()

                // email-service with 2 instances
                val emailService1 = factory.create(ServiceIdentity("email-service"))
                val emailService2 = factory.create(ServiceIdentity("email-service"))

                // notification-service with 1 instance
                val notificationService = factory.create(ServiceIdentity("notification-service"))

                emailService1.connect()
                emailService2.connect()
                notificationService.connect()

                val eventType = TestFactory.createEventType("user.created")
                val emailMessages1 = mutableListOf<TransportMessage>()
                val emailMessages2 = mutableListOf<TransportMessage>()
                val notificationMessages = mutableListOf<TransportMessage>()

                emailService1.subscribe(eventType, TestFactory.createHandler { emailMessages1.add(it) })
                emailService2.subscribe(eventType, TestFactory.createHandler { emailMessages2.add(it) })
                notificationService.subscribe(eventType, TestFactory.createHandler { notificationMessages.add(it) })

                // Send multiple messages
                repeat(TestConstants.TEST_MESSAGE_COUNT) { i ->
                    val message = TestFactory.createMessage(id = "msg-$i", eventType = eventType.value)
                    emailService1.send(message)
                    delay(TestConstants.TEST_DELAY_MS)
                }

                // Each service gets all messages, but distributed across instances
                emailMessages1.size + emailMessages2.size shouldBe TestConstants.TEST_MESSAGE_COUNT
                notificationMessages.size shouldBe TestConstants.TEST_MESSAGE_COUNT

                // Both email service instances should receive at least one message (statistically)
                // With 10 messages and random distribution, probability of one instance getting 0 is very low
                emailMessages1.size shouldBeGreaterThan 0
                emailMessages2.size shouldBeGreaterThan 0

                emailService1.disconnect()
                emailService2.disconnect()
                notificationService.disconnect()
            }

            test("should distribute messages randomly across instances") {
                val factory = InMemoryTransportFactory()

                val instance1 = factory.create(ServiceIdentity("service"))
                val instance2 = factory.create(ServiceIdentity("service"))
                val instance3 = factory.create(ServiceIdentity("service"))

                instance1.connect()
                instance2.connect()
                instance3.connect()

                val eventType = TestFactory.createEventType()
                var count1 = 0
                var count2 = 0
                var count3 = 0

                instance1.subscribe(eventType, TestFactory.createHandler { count1++ })
                instance2.subscribe(eventType, TestFactory.createHandler { count2++ })
                instance3.subscribe(eventType, TestFactory.createHandler { count3++ })

                // Send many messages to verify random distribution
                repeat(TestConstants.LARGE_MESSAGE_COUNT) {
                    instance1.send(TestFactory.createMessage())
                }
                delay(100)

                // All instances should receive at least some messages
                count1 shouldBeGreaterThan 0
                count2 shouldBeGreaterThan 0
                count3 shouldBeGreaterThan 0

                // Total should match sent messages
                count1 + count2 + count3 shouldBe TestConstants.LARGE_MESSAGE_COUNT

                instance1.disconnect()
                instance2.disconnect()
                instance3.disconnect()
            }

            test("should not deliver to services with no active instances") {
                val factory = InMemoryTransportFactory()

                val service1 = factory.create(ServiceIdentity("active-service"))
                val service2 = factory.create(ServiceIdentity("inactive-service"))

                service1.connect()
                service2.connect()

                val eventType = TestFactory.createEventType()
                var activeCount = 0
                var inactiveCount = 0

                service1.subscribe(eventType, TestFactory.createHandler { activeCount++ })
                service2.subscribe(eventType, TestFactory.createHandler { inactiveCount++ })

                // Disconnect the inactive service
                service2.disconnect()

                service1.send(TestFactory.createMessage())
                delay(TestConstants.TEST_DELAY_MS)

                activeCount shouldBe 1
                inactiveCount shouldBe 0

                service1.disconnect()
            }

            test("should handle dynamic instance addition") {
                val factory = InMemoryTransportFactory()

                val instance1 = factory.create(ServiceIdentity("service"))
                instance1.connect()

                val eventType = TestFactory.createEventType()
                var count1 = 0
                var count2 = 0

                instance1.subscribe(eventType, TestFactory.createHandler { count1++ })

                // Send first batch
                repeat(5) {
                    instance1.send(TestFactory.createMessage())
                }
                delay(TestConstants.TEST_DELAY_MS)

                count1 shouldBe 5

                // Add second instance
                val instance2 = factory.create(ServiceIdentity("service"))
                instance2.connect()
                instance2.subscribe(eventType, TestFactory.createHandler { count2++ })

                // Send second batch
                repeat(10) {
                    instance1.send(TestFactory.createMessage())
                }
                delay(TestConstants.TEST_DELAY_MS)

                // Messages should now be distributed
                count1 + count2 shouldBe 15
                count2 shouldBeGreaterThan 0

                instance1.disconnect()
                instance2.disconnect()
            }
        }

        context("Service Isolation") {
            test("should isolate messages when using different factories") {
                val factory1 = InMemoryTransportFactory()
                val factory2 = InMemoryTransportFactory()

                val transport1 = factory1.create(TestFactory.createServiceIdentity("service1"))
                val transport2 = factory2.create(TestFactory.createServiceIdentity("service2"))

                transport1.connect()
                transport2.connect()

                val eventType = TestFactory.createEventType()
                var count1 = 0
                var count2 = 0

                transport1.subscribe(eventType, TestFactory.createHandler { count1++ })
                transport2.subscribe(eventType, TestFactory.createHandler { count2++ })

                // Send from factory1
                transport1.send(TestFactory.createMessage())
                delay(TestConstants.TEST_DELAY_MS)

                // Only transport1 should receive (different message buses)
                count1 shouldBe 1
                count2 shouldBe 0

                transport1.disconnect()
                transport2.disconnect()
            }

            test("should share messages when using same factory") {
                val factory = InMemoryTransportFactory()

                val transport1 = factory.create(TestFactory.createServiceIdentity("service1"))
                val transport2 = factory.create(TestFactory.createServiceIdentity("service2"))

                transport1.connect()
                transport2.connect()

                val eventType = TestFactory.createEventType()
                var count1 = 0
                var count2 = 0

                transport1.subscribe(eventType, TestFactory.createHandler { count1++ })
                transport2.subscribe(eventType, TestFactory.createHandler { count2++ })

                transport1.send(TestFactory.createMessage())
                delay(TestConstants.TEST_DELAY_MS)

                // Both should receive (shared message bus)
                count1 shouldBe 1
                count2 shouldBe 1

                transport1.disconnect()
                transport2.disconnect()
            }
        }
    })
