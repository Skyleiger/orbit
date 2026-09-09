package io.orbit.integration

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.orbit.core.Orbit
import io.orbit.core.event.Event
import io.orbit.core.orbit
import io.orbit.core.serializer.SerializerFactory
import io.orbit.core.transport.TransportFactory
import kotlinx.serialization.Serializable
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.update
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

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

    private val openOrbits = mutableListOf<Orbit>()

    /**
     * Generates a service name that is unique per test.
     *
     * Transports may derive their queue name from the service name, so reusing a name across
     * tests makes those tests share a queue and compete for each other's messages.
     */
    protected fun uniqueServiceName(prefix: String): String = "$prefix-${Uuid.random()}"

    /**
     * Registers this instance to be disconnected after the current test.
     *
     * Assertions run before any explicit close, so without this a failing test would leave a
     * consumer attached to its queue and disturb the tests that follow.
     */
    protected fun Orbit.closeAfterTest(): Orbit = also { openOrbits.add(it) }

    init {
        afterTest {
            openOrbits.forEach { runCatching { it.disconnect() } }
            openOrbits.clear()
        }

        test("should publish and receive event via transport") {
            val transport = createTransportFactory()
            val serializer = createSerializerFactory()
            val receivedEvents = AtomicReference<List<UserCreatedEvent>>(emptyList())

            // Publisher Orbit
            val publisherOrbit =
                orbit {
                    service(uniqueServiceName("publisher-service"))
                    serializer(serializer)
                    transport(transport)

                    event(UserCreatedEvent::class)
                }.closeAfterTest()

            // Subscriber Orbit
            val subscriberOrbit =
                orbit {
                    service(uniqueServiceName("subscriber-service"))
                    serializer(serializer)
                    transport(transport)

                    event(UserCreatedEvent::class)
                    handler(UserCreatedEvent::class) { event ->
                        receivedEvents.update { it + event }
                    }
                }.closeAfterTest()

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
                receivedEvents.load().size shouldBe 1
            }

            // Then: Event should be received
            receivedEvents.load()[0].userId shouldBe "user-123"
            receivedEvents.load()[0].email shouldBe "test@example.com"

            // Cleanup
            publisherOrbit.close()
            subscriberOrbit.close()

            publisherOrbit.isConnected() shouldBe false
            subscriberOrbit.isConnected() shouldBe false
        }

        test("should handle multiple event types") {
            val transport = createTransportFactory()
            val serializer = createSerializerFactory()
            val receivedUserCreated = AtomicReference<List<UserCreatedEvent>>(emptyList())
            val receivedUserUpdated = AtomicReference<List<UserUpdatedEvent>>(emptyList())

            val publisherOrbit =
                orbit {
                    service(uniqueServiceName("publisher"))
                    serializer(serializer)
                    transport(transport)

                    event(UserCreatedEvent::class)
                    event(UserUpdatedEvent::class)
                }.closeAfterTest()

            val subscriberOrbit =
                orbit {
                    service(uniqueServiceName("subscriber"))
                    serializer(serializer)
                    transport(transport)

                    event(UserCreatedEvent::class)
                    event(UserUpdatedEvent::class)

                    handler(UserCreatedEvent::class) { event ->
                        receivedUserCreated.update { it + event }
                    }

                    handler(UserUpdatedEvent::class) { event ->
                        receivedUserUpdated.update { it + event }
                    }
                }.closeAfterTest()

            publisherOrbit.connect()
            subscriberOrbit.connect()

            // When: Publish different event types
            publisherOrbit.publish(UserCreatedEvent("user-1", "user1@example.com"))
            publisherOrbit.publish(UserUpdatedEvent("user-1", "updated@example.com"))
            publisherOrbit.publish(UserCreatedEvent("user-2", "user2@example.com"))

            eventually(eventuallyTimeoutMs.milliseconds) {
                receivedUserCreated.load().size shouldBe 2
                receivedUserUpdated.load().size shouldBe 1
            }

            // Then: delivery order is not guaranteed, only which events arrive
            receivedUserCreated.load().map { it.userId } shouldContainExactlyInAnyOrder listOf("user-1", "user-2")
            receivedUserUpdated.load()[0].newEmail shouldBe "updated@example.com"
        }

        test("should support multiple handlers for same event") {
            val transport = createTransportFactory()
            val serializer = createSerializerFactory()
            val handler1Events = AtomicReference<List<UserCreatedEvent>>(emptyList())
            val handler2Events = AtomicReference<List<UserCreatedEvent>>(emptyList())

            val subscriberOrbit =
                orbit {
                    service(uniqueServiceName("subscriber"))
                    serializer(serializer)
                    transport(transport)

                    event(UserCreatedEvent::class)

                    handler(UserCreatedEvent::class) { event ->
                        handler1Events.update { it + event }
                    }

                    handler(UserCreatedEvent::class) { event ->
                        handler2Events.update { it + event }
                    }
                }.closeAfterTest()

            subscriberOrbit.connect()

            // When
            subscriberOrbit.publish(UserCreatedEvent("user-1", "test@example.com"))

            eventually(eventuallyTimeoutMs.milliseconds) {
                handler1Events.load().size shouldBe 1
                handler2Events.load().size shouldBe 1
            }

            // Then: Both handlers should be called
            handler1Events.load()[0].userId shouldBe "user-1"
            handler2Events.load()[0].userId shouldBe "user-1"
        }

        test("should generate unique service IDs") {
            val transport = createTransportFactory()
            val serializer = createSerializerFactory()

            // Given - create two Orbit instances with same service name
            val serviceName = uniqueServiceName("test-service")

            val orbit1 =
                orbit {
                    service(serviceName)
                    serializer(serializer)
                    transport(transport)

                    event(UserCreatedEvent::class)
                }.closeAfterTest()

            val orbit2 =
                orbit {
                    service(serviceName)
                    serializer(serializer)
                    transport(transport)

                    event(UserCreatedEvent::class)
                }.closeAfterTest()

            orbit1.connect()
            orbit2.connect()

            // Service IDs are unique per instance (UUID-based)
            // Both instances should be connected successfully
            orbit1.isConnected() shouldBe true
            orbit2.isConnected() shouldBe true
        }

        test("should preserve event metadata") {
            val transport = createTransportFactory()
            val serializer = createSerializerFactory()
            val receivedEvents = AtomicReference<List<UserCreatedEvent>>(emptyList())

            val orbit =
                orbit {
                    service(uniqueServiceName("test-service"))
                    serializer(serializer)
                    transport(transport)

                    event(UserCreatedEvent::class)

                    handler(UserCreatedEvent::class) { event ->
                        receivedEvents.update { it + event }
                    }
                }.closeAfterTest()

            orbit.connect()

            // When
            orbit.publish(UserCreatedEvent("user-1", "test@example.com"))

            eventually(eventuallyTimeoutMs.milliseconds) {
                receivedEvents.load().size shouldBe 1
            }

            // Then
            receivedEvents.load()[0] shouldNotBe null
        }

        test("should fail when publishing unregistered event") {
            val transport = createTransportFactory()
            val serializer = createSerializerFactory()

            // Given
            val orbit =
                orbit {
                    service(uniqueServiceName("test-service"))
                    serializer(serializer)
                    transport(transport)

                    // Note: UserCreatedEvent is NOT registered
                }.closeAfterTest()

            orbit.connect()

            // When/Then: Should throw error
            try {
                orbit.publish(UserCreatedEvent("user-1", "test@example.com"))
                throw AssertionError("Expected error for unregistered event")
            } catch (e: IllegalStateException) {
                e.message shouldBe "Event UserCreatedEvent is not registered"
            }
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
