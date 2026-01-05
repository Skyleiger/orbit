package io.orbit.spring.integration

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.orbit.core.Orbit
import io.orbit.core.event.Event
import io.orbit.spring.annotation.EventHandler
import kotlinx.coroutines.delay
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import org.springframework.test.context.TestPropertySource
import kotlin.time.Duration.Companion.seconds

/**
 * Integration tests for Orbit Spring Boot Starter.
 *
 * These tests verify the complete integration of Orbit with Spring Boot:
 * - Auto-configuration from META-INF/spring
 * - Event handler discovery and registration
 * - Event publishing and receiving with multiple handlers
 * - Support for suspend functions
 * - Connection lifecycle management
 *
 * The Spring context is created once for all tests, improving test performance.
 */
@SpringBootTest(classes = [TestApplication::class])
@TestPropertySource(
    properties = [
        "orbit.service.name=integration-test",
    ],
)
@ApplyExtension(SpringExtension::class)
class OrbitSpringBootIntegrationTest(
    private val orbit: Orbit,
    private val firstHandler: FirstEventHandler,
    private val secondHandler: SecondEventHandler,
    private val suspendHandler: SuspendEventHandler,
) : FunSpec({

        beforeEach {
            // Clean up handler state before each test
            firstHandler.receivedEvents.clear()
            secondHandler.receivedEvents.clear()
            suspendHandler.receivedEvents.clear()
        }

        context("Event Publishing and Handling") {
            test("should publish and receive events with single handler") {
                orbit.publish(IntegrationEvent("test-message"))

                eventually(2.seconds) {
                    firstHandler.receivedEvents shouldHaveSize 1
                    firstHandler.receivedEvents[0].message shouldBe "test-message"
                }
            }

            test("should handle events with multiple handlers") {
                orbit.publish(IntegrationEvent("multi-handler"))

                eventually(2.seconds) {
                    firstHandler.receivedEvents shouldHaveSize 1
                    firstHandler.receivedEvents[0].message shouldBe "multi-handler"
                    secondHandler.receivedEvents shouldHaveSize 1
                    secondHandler.receivedEvents[0].message shouldBe "multi-handler"
                }
            }

            test("should support suspend event handlers") {
                orbit.publish(IntegrationEvent("suspend-test"))

                eventually(2.seconds) {
                    suspendHandler.receivedEvents shouldHaveSize 1
                    suspendHandler.receivedEvents[0].message shouldBe "suspend-test"
                }
            }

            test("should handle multiple sequential events") {
                orbit.publish(IntegrationEvent("event-1"))
                orbit.publish(IntegrationEvent("event-2"))
                orbit.publish(IntegrationEvent("event-3"))

                eventually(2.seconds) {
                    firstHandler.receivedEvents shouldHaveSize 3
                    firstHandler.receivedEvents.map { it.message } shouldBe listOf("event-1", "event-2", "event-3")
                }
            }
        }

        context("Spring Boot Auto-Configuration") {
            test("should create Orbit bean through auto-configuration") {
                orbit shouldNotBe null
            }

            test("should auto-discover event handlers") {
                firstHandler shouldNotBe null
                secondHandler shouldNotBe null
                suspendHandler shouldNotBe null
            }
        }

        context("Connection Lifecycle") {
            test("should be connected after spec setup") {
                orbit.isConnected() shouldBe true
            }
        }
    })

// ============================================================================
// Test Application (simulates a real Spring Boot app)
// ============================================================================

/**
 * Test application for integration tests.
 *
 * This application is in a separate package to avoid conflicts with other test configurations.
 * It enables auto-configuration by using @SpringBootApplication, which:
 * - Loads all configurations from META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 * - Scans for @Component beans in the io.orbit.spring.integration package
 */
@SpringBootApplication
class TestApplication

// ============================================================================
// Test Events
// ============================================================================

@Event("integration.test")
data class IntegrationEvent(
    val message: String,
)

// ============================================================================
// Test Event Handlers
// ============================================================================

@Component
class FirstEventHandler {
    val receivedEvents = mutableListOf<IntegrationEvent>()

    @Suppress("unused") // invoked via reflection
    @EventHandler
    fun handle(event: IntegrationEvent) {
        receivedEvents.add(event)
    }
}

@Component
class SecondEventHandler {
    val receivedEvents = mutableListOf<IntegrationEvent>()

    @Suppress("unused") // invoked via reflection
    @EventHandler
    fun handle(event: IntegrationEvent) {
        receivedEvents.add(event)
    }
}

@Component
class SuspendEventHandler {
    val receivedEvents = mutableListOf<IntegrationEvent>()

    @Suppress("unused") // invoked via reflection
    @EventHandler
    suspend fun handle(event: IntegrationEvent) {
        delay(10) // Simulate async work
        receivedEvents.add(event)
    }
}
