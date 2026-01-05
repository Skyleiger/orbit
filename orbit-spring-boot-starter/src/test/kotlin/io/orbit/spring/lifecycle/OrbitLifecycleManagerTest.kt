package io.orbit.spring.lifecycle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.spyk
import io.orbit.core.Orbit

/**
 * Test suite for [OrbitLifecycleManager].
 *
 * Tests the Spring SmartLifecycle integration for Orbit:
 * - Lifecycle state transitions (start/stop)
 * - SmartLifecycle contract compliance
 * - Idempotent operations
 * - Configuration properties
 */
class OrbitLifecycleManagerTest :
    FunSpec({

        context("Lifecycle State Transitions") {
            test("should start Orbit and transition to running state") {
                val orbit = spyk(TestOrbit())
                val manager = OrbitLifecycleManager(orbit, autoStartup = false)

                manager.isRunning shouldBe false

                manager.start()

                manager.isRunning shouldBe true
                coVerify(exactly = 1) { orbit.connect() }
            }

            test("should stop Orbit and transition to stopped state") {
                val orbit = spyk(TestOrbit())
                val manager = OrbitLifecycleManager(orbit, autoStartup = false)

                manager.start()
                manager.isRunning shouldBe true

                manager.stop()

                manager.isRunning shouldBe false
                coVerify(exactly = 1) { orbit.disconnect() }
            }

            test("should handle complete lifecycle: start -> stop -> start") {
                val orbit = spyk(TestOrbit())
                val manager = OrbitLifecycleManager(orbit, autoStartup = false)

                // First cycle
                manager.start()
                manager.isRunning shouldBe true

                manager.stop()
                manager.isRunning shouldBe false

                // Second cycle
                manager.start()
                manager.isRunning shouldBe true

                coVerify(exactly = 2) { orbit.connect() }
                coVerify(exactly = 1) { orbit.disconnect() }
            }
        }

        context("SmartLifecycle Contract") {
            test("should return configured autoStartup value (true)") {
                val orbit = mockk<Orbit>()
                val manager = OrbitLifecycleManager(orbit, autoStartup = true)

                manager.isAutoStartup shouldBe true
                confirmVerified(orbit)
            }

            test("should return configured autoStartup value (false)") {
                val orbit = mockk<Orbit>()
                val manager = OrbitLifecycleManager(orbit, autoStartup = false)

                manager.isAutoStartup shouldBe false
                confirmVerified(orbit)
            }

            test("should return correct phase value for late startup") {
                val orbit = mockk<Orbit>()
                val manager = OrbitLifecycleManager(orbit, autoStartup = false)

                // Phase near MAX_VALUE means start late, stop early
                // This ensures Orbit starts after most other components
                manager.phase shouldBe Integer.MAX_VALUE - 1000
                confirmVerified(orbit)
            }

            test("should reflect Orbit connection state in isRunning()") {
                val orbit = spyk(TestOrbit())
                val manager = OrbitLifecycleManager(orbit, autoStartup = false)

                // Initial state
                manager.isRunning shouldBe false

                // After connect
                orbit.connect()
                manager.isRunning shouldBe true

                // After disconnect
                orbit.disconnect()
                manager.isRunning shouldBe false
            }
        }
    })

/**
 * Simple test implementation of Orbit for testing lifecycle behavior.
 */
private class TestOrbit(
    initiallyConnected: Boolean = false,
) : Orbit {
    private var connected = initiallyConnected

    override suspend fun connect() {
        connected = true
    }

    override suspend fun disconnect() {
        connected = false
    }

    override suspend fun isConnected(): Boolean = connected

    override suspend fun publish(event: Any) {
        // Not needed for lifecycle tests
    }

    override fun close() {
        // Not needed for lifecycle tests
    }
}
