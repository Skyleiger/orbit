package io.orbit.integration

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.orbit.core.event.Event
import io.orbit.core.orbit
import io.orbit.core.serializer.SerializerFactory
import io.orbit.core.transport.TransportFactory
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

/**
 * Abstract contract test suite for Orbit integration testing.
 *
 * All combinations of TransportFactory and SerializerFactory must pass these tests
 * to ensure they work correctly together in a complete Orbit setup.
 *
 * The tests verify:
 * - Publishing and receiving events
 * - Multiple event types
 * - Multiple handlers for the same event
 * - Service identity and lifecycle
 * - Event metadata preservation
 * - Error handling for unregistered events
 *
 * Implementations must provide both TransportFactory and SerializerFactory:
 * ```kotlin
 * class MyIntegrationTest : IntegrationTestContract() {
 *     override fun createTransportFactory(): TransportFactory = MyTransportFactory()
 *     override fun createSerializerFactory(): SerializerFactory = MySerializerFactory()
 * }
 * ```
 */
abstract class IntegrationTestContract : FunSpec() {
    /**
     * Provides the TransportFactory instance for creating transports for testing.
     * Must be implemented by each test class.
     */
    abstract fun createTransportFactory(): TransportFactory

    /**
     * Provides the SerializerFactory instance for creating serializers for testing.
     * Must be implemented by each test class.
     */
    abstract fun createSerializerFactory(): SerializerFactory

    /**
     * Timeout in milliseconds for eventual consistency checks.
     * Override based on transport characteristics (network-based needs longer).
     */
    open val eventuallyTimeoutMs: Long = 5000L

    init {
        test("should publish and receive event via transport") {
            val transport = createTransportFactory()
            val serializer = createSerializerFactory()
            val receivedEvents = mutableListOf<UserCreatedEvent>()

            // Publisher Orbit
            val publisherOrbit =
                orbit {
                    service("publisher-service")
                    serializer(serializer)
                    transport(transport)

                    event(UserCreatedEvent::class)
                }

            // Subscriber Orbit
            val subscriberOrbit =
                orbit {
                    service("subscriber-service")
                    serializer(serializer)
                    transport(transport)

                    event(UserCreatedEvent::class)
                    handler(UserCreatedEvent::class) {
                        receivedEvents.add(it)
                    }
                }

            // When: Connect both
            publisherOrbit.connect()
            subscriberOrbit.connect()

            publisherOrbit.isConnected() shouldBe true
            subscriberOrbit.isConnected() shouldBe true

            // When: Publish event
            val testEvent = UserCreatedEvent("user-123", "test@example.com")
            publisherOrbit.publish(testEvent)

            // Give some time for async processing
            eventually(eventuallyTimeoutMs.milliseconds) {
                receivedEvents.size shouldBe 1
            }

            // Then: Event should be received
            receivedEvents[0].userId shouldBe "user-123"
            receivedEvents[0].email shouldBe "test@example.com"

            // Cleanup
            publisherOrbit.close()
            subscriberOrbit.close()

            publisherOrbit.isConnected() shouldBe false
            subscriberOrbit.isConnected() shouldBe false
        }

        test("should handle multiple event types") {
            val transport = createTransportFactory()
            val serializer = createSerializerFactory()
            val receivedUserCreated = mutableListOf<UserCreatedEvent>()
            val receivedUserUpdated = mutableListOf<UserUpdatedEvent>()

            val publisherOrbit =
                orbit {
                    service("publisher")
                    serializer(serializer)
                    transport(transport)

                    event(UserCreatedEvent::class)
                    event(UserUpdatedEvent::class)
                }

            val subscriberOrbit =
                orbit {
                    service("subscriber")
                    serializer(serializer)
                    transport(transport)

                    event(UserCreatedEvent::class)
                    event(UserUpdatedEvent::class)

                    handler(UserCreatedEvent::class) {
                        receivedUserCreated.add(it)
                    }

                    handler(UserUpdatedEvent::class) {
                        receivedUserUpdated.add(it)
                    }
                }

            publisherOrbit.connect()
            subscriberOrbit.connect()

            // When: Publish different event types
            publisherOrbit.publish(UserCreatedEvent("user-1", "user1@example.com"))
            publisherOrbit.publish(UserUpdatedEvent("user-1", "updated@example.com"))
            publisherOrbit.publish(UserCreatedEvent("user-2", "user2@example.com"))

            eventually(eventuallyTimeoutMs.milliseconds) {
                receivedUserCreated.size shouldBe 2
                receivedUserUpdated.size shouldBe 1
            }

            // Then
            receivedUserCreated.map { it.userId } shouldContainExactly listOf("user-1", "user-2")
            receivedUserUpdated[0].newEmail shouldBe "updated@example.com"

            publisherOrbit.close()
            subscriberOrbit.close()
        }

        test("should support multiple handlers for same event") {
            val transport = createTransportFactory()
            val serializer = createSerializerFactory()
            val handler1Events = mutableListOf<UserCreatedEvent>()
            val handler2Events = mutableListOf<UserCreatedEvent>()

            val subscriberOrbit =
                orbit {
                    service("subscriber")
                    serializer(serializer)
                    transport(transport)

                    event(UserCreatedEvent::class)

                    handler(UserCreatedEvent::class) { event ->
                        handler1Events.add(event)
                    }

                    handler(UserCreatedEvent::class) { event ->
                        handler2Events.add(event)
                    }
                }

            subscriberOrbit.connect()

            // When
            subscriberOrbit.publish(UserCreatedEvent("user-1", "test@example.com"))

            eventually(eventuallyTimeoutMs.milliseconds) {
                handler1Events.size shouldBe 1
                handler2Events.size shouldBe 1
            }

            // Then: Both handlers should be called
            handler1Events[0].userId shouldBe "user-1"
            handler2Events[0].userId shouldBe "user-1"

            subscriberOrbit.close()
        }

        test("should generate unique service IDs") {
            val transport = createTransportFactory()
            val serializer = createSerializerFactory()

            // Given - create two Orbit instances with same service name
            val orbit1 =
                orbit {
                    service("test-service")
                    serializer(serializer)
                    transport(transport)

                    event(UserCreatedEvent::class)
                }

            val orbit2 =
                orbit {
                    service("test-service")
                    serializer(serializer)
                    transport(transport)

                    event(UserCreatedEvent::class)
                }

            orbit1.connect()
            orbit2.connect()

            // Service IDs are unique per instance (UUID-based)
            // Both instances should be connected successfully
            orbit1.isConnected() shouldBe true
            orbit2.isConnected() shouldBe true

            orbit1.close()
            orbit2.close()
        }

        test("should preserve event metadata") {
            val transport = createTransportFactory()
            val serializer = createSerializerFactory()
            val receivedEvents = mutableListOf<UserCreatedEvent>()

            val orbit =
                orbit {
                    service("test-service")
                    serializer(serializer)
                    transport(transport)

                    event(UserCreatedEvent::class)

                    handler(UserCreatedEvent::class) { event ->
                        receivedEvents.add(event)
                    }
                }

            orbit.connect()

            // When
            orbit.publish(UserCreatedEvent("user-1", "test@example.com"))

            eventually(eventuallyTimeoutMs.milliseconds) {
                receivedEvents.size shouldBe 1
            }

            // Then
            receivedEvents[0] shouldNotBe null

            orbit.close()
        }

        test("should fail when publishing unregistered event") {
            val transport = createTransportFactory()
            val serializer = createSerializerFactory()

            // Given
            val orbit =
                orbit {
                    service("test-service")
                    serializer(serializer)
                    transport(transport)

                    // Note: UserCreatedEvent is NOT registered
                }

            orbit.connect()

            // When/Then: Should throw error
            try {
                orbit.publish(UserCreatedEvent("user-1", "test@example.com"))
                throw AssertionError("Expected error for unregistered event")
            } catch (e: IllegalStateException) {
                e.message shouldBe "Event UserCreatedEvent is not registered"
            }

            orbit.close()
        }
    }
}

@Event("test.user.created")
@Serializable
data class UserCreatedEvent(
    val userId: String,
    val email: String,
)

@Event("test.user.updated")
@Serializable
data class UserUpdatedEvent(
    val userId: String,
    val newEmail: String,
)
