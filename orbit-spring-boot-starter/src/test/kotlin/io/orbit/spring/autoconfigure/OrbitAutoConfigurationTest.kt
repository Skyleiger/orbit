package io.orbit.spring.autoconfigure

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.orbit.core.Orbit
import io.orbit.core.event.Event
import io.orbit.core.orbit
import io.orbit.serialization.jackson.JacksonSerializerFactory
import io.orbit.spring.annotation.EventHandler
import io.orbit.spring.autoconfigure.serialization.JacksonSerializerAutoConfiguration
import io.orbit.spring.autoconfigure.transport.InMemoryTransportAutoConfiguration
import io.orbit.spring.lifecycle.OrbitLifecycleManager
import io.orbit.transport.inmemory.InMemoryTransportFactory
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import kotlin.time.Duration.Companion.seconds

/**
 * Test suite for [OrbitAutoConfiguration].
 *
 * These tests verify the Spring Boot auto-configuration behavior:
 * - Bean creation and dependency injection
 * - Service name resolution from properties
 * - Event handler discovery via @EventHandler annotation
 * - Conditional bean creation (@ConditionalOnMissingBean)
 * - Lifecycle management integration
 */
class OrbitAutoConfigurationTest :
    FunSpec({

        val contextRunner =
            ApplicationContextRunner()
                .withConfiguration(
                    AutoConfigurations.of(
                        OrbitAutoConfiguration::class.java,
                        InMemoryTransportAutoConfiguration::class.java,
                        JacksonSerializerAutoConfiguration::class.java,
                    ),
                )

        context("Bean Creation and Dependencies") {
            test("should create all required beans when properly configured") {
                contextRunner
                    .withPropertyValues("orbit.service.name=test-service")
                    .run { context ->
                        context.getBean<Orbit>() shouldNotBe null
                        context.getBean<OrbitLifecycleManager>() shouldNotBe null
                    }
            }

            test("should inject SerializerFactory and TransportFactory into Orbit") {
                contextRunner
                    .withPropertyValues("orbit.service.name=test-service")
                    .run { context ->
                        val orbit = context.getBean<Orbit>()

                        // Verify Orbit can connect (proves transport and serializer are working)
                        runBlocking {
                            orbit.connect()
                            orbit.isConnected() shouldBe true
                            orbit.disconnect()
                        }
                    }
            }

            test("should respect @ConditionalOnMissingBean for Orbit") {
                contextRunner
                    .withPropertyValues("orbit.service.name=test-service")
                    .withUserConfiguration(CustomOrbitConfig::class.java)
                    .run { context ->
                        val orbit = context.getBean<Orbit>()
                        orbit shouldNotBe null

                        // Autoconfigured bean is "orbit" -> must NOT be present
                        context.containsBean("orbit") shouldBe false

                        // User bean should be present
                        context.containsBean("customOrbit") shouldBe true
                    }
            }

            test("should apply customizer to Orbit") {
                contextRunner
                    .withPropertyValues("orbit.service.name=test-service")
                    .withUserConfiguration(SingleCustomizerConfig::class.java)
                    .run { context ->
                        context.getBean<Orbit>() shouldNotBe null
                        val customizer = context.getBean<SingleCustomizerConfig>()
                        customizer.called shouldBe true
                    }
            }

            test("should apply multiple customizers in order") {
                contextRunner
                    .withPropertyValues("orbit.service.name=test-service")
                    .withUserConfiguration(OrderedCustomizersConfig::class.java)
                    .run { context ->
                        val config = context.getBean<OrderedCustomizersConfig>()
                        config.callOrder shouldBe listOf("first", "second")
                    }
            }
        }

        context("Service Name Resolution") {
            test("should use orbit.service.name when configured") {
                contextRunner
                    .withPropertyValues("orbit.service.name=my-service")
                    .run { context ->
                        context.startupFailure shouldBe null
                        context.getBean<Orbit>() shouldNotBe null
                    }
            }

            test("should use spring.application.name as service name when orbit.service.name is not configured") {
                contextRunner
                    .withPropertyValues("spring.application.name=my-app")
                    .run { context ->
                        context.startupFailure shouldBe null
                        context.getBean<Orbit>() shouldNotBe null
                    }
            }

            test("should fail when no service name is configured") {
                contextRunner
                    .run { context ->
                        context.startupFailure shouldNotBe null
                        context.startupFailure?.message shouldContain "orbit.service.name"
                    }
            }
        }

        context("Event Handler Discovery") {
            test("should discover @EventHandler annotated methods") {
                contextRunner
                    .withPropertyValues("orbit.service.name=test-service")
                    .withUserConfiguration(SingleEventHandlerConfig::class.java)
                    .run { context ->
                        val orbit = context.getBean<Orbit>()
                        val handler = context.getBean<SingleEventHandler>()

                        runBlocking {
                            orbit.publish(TestEvent("test-message"))

                            eventually(2.seconds) {
                                handler.receivedEvents shouldHaveSize 1
                                handler.receivedEvents[0].message shouldBe "test-message"
                            }
                        }
                    }
            }

            test("should discover multiple @EventHandler methods in same bean") {
                contextRunner
                    .withPropertyValues("orbit.service.name=test-service")
                    .withUserConfiguration(MultipleHandlersInBeanConfig::class.java)
                    .run { context ->
                        val orbit = context.getBean<Orbit>()
                        val handler = context.getBean<MultipleHandlersBean>()

                        runBlocking {
                            orbit.publish(TestEvent("event-1"))
                            orbit.publish(AnotherTestEvent(42))

                            eventually(2.seconds) {
                                handler.testEvents shouldHaveSize 1
                                handler.anotherEvents shouldHaveSize 1
                            }
                        }
                    }
            }

            test("should discover handlers in multiple beans for same event type") {
                contextRunner
                    .withPropertyValues("orbit.service.name=test-service")
                    .withUserConfiguration(MultipleBeansConfig::class.java)
                    .run { context ->
                        val orbit = context.getBean<Orbit>()
                        val handler1 = context.getBean<FirstTestHandler>()
                        val handler2 = context.getBean<SecondTestHandler>()

                        runBlocking {
                            orbit.publish(TestEvent("multi-handler"))

                            eventually(2.seconds) {
                                handler1.receivedEvents shouldHaveSize 1
                                handler2.receivedEvents shouldHaveSize 1
                            }
                        }
                    }
            }

            test("should support suspend event handler functions") {
                contextRunner
                    .withPropertyValues("orbit.service.name=test-service")
                    .withUserConfiguration(SuspendHandlerConfig::class.java)
                    .run { context ->
                        val orbit = context.getBean<Orbit>()
                        val handler = context.getBean<SuspendEventHandler>()

                        runBlocking {
                            orbit.publish(TestEvent("suspend-test"))

                            eventually(2.seconds) {
                                handler.receivedEvents shouldHaveSize 1
                            }
                        }
                    }
            }
        }

        context("Lifecycle Management") {
            test("should create LifecycleManager with autoStartup from properties") {
                contextRunner
                    .withPropertyValues("orbit.service.name=test-service")
                    .run { context ->
                        val lifecycleManager = context.getBean<OrbitLifecycleManager>()
                        lifecycleManager shouldNotBe null
                        lifecycleManager.isAutoStartup shouldBe true
                    }
            }

            test("should pass autoStartup property to LifecycleManager") {
                contextRunner
                    .withPropertyValues(
                        "orbit.service.name=test-service",
                        "orbit.auto-startup=false",
                    ).run { context ->
                        val lifecycleManager = context.getBean<OrbitLifecycleManager>()
                        lifecycleManager.isAutoStartup shouldBe false
                    }
            }
        }
    })

// ============================================================================
// Test Configurations
// ============================================================================

@Configuration
class SingleEventHandlerConfig {
    @Bean
    fun singleEventHandler() = SingleEventHandler()
}

@Component
class SingleEventHandler {
    val receivedEvents = mutableListOf<TestEvent>()

    @Suppress("unused")
    @EventHandler
    fun handleTestEvent(event: TestEvent) {
        receivedEvents.add(event)
    }
}

@Configuration
class MultipleHandlersInBeanConfig {
    @Bean
    fun multipleHandlersBean() = MultipleHandlersBean()
}

@Component
class MultipleHandlersBean {
    val testEvents = mutableListOf<TestEvent>()
    val anotherEvents = mutableListOf<AnotherTestEvent>()

    @Suppress("unused")
    @EventHandler
    fun handleTestEvent(event: TestEvent) {
        testEvents.add(event)
    }

    @Suppress("unused")
    @EventHandler
    fun handleAnotherEvent(event: AnotherTestEvent) {
        anotherEvents.add(event)
    }
}

@Configuration
class MultipleBeansConfig {
    @Bean
    fun firstTestHandler() = FirstTestHandler()

    @Bean
    fun secondTestHandler() = SecondTestHandler()
}

@Component
class FirstTestHandler {
    val receivedEvents = mutableListOf<TestEvent>()

    @Suppress("unused")
    @EventHandler
    fun handleTestEvent(event: TestEvent) {
        receivedEvents.add(event)
    }
}

@Component
class SecondTestHandler {
    val receivedEvents = mutableListOf<TestEvent>()

    @Suppress("unused")
    @EventHandler
    fun handleTestEvent(event: TestEvent) {
        receivedEvents.add(event)
    }
}

@Configuration
class SuspendHandlerConfig {
    @Bean
    fun suspendEventHandler() = SuspendEventHandler()
}

@Component
class SuspendEventHandler {
    val receivedEvents = mutableListOf<TestEvent>()

    @Suppress("unused")
    @EventHandler
    suspend fun handleTestEvent(event: TestEvent) {
        receivedEvents.add(event)
    }
}

// ============================================================================
// Custom Bean Configurations
// ============================================================================

@Configuration
class CustomOrbitConfig {
    @Bean
    fun customOrbit(): Orbit =
        orbit {
            service("custom-service")
            transport(InMemoryTransportFactory())
            serializer(JacksonSerializerFactory())
        }
}

// ============================================================================
// Test Events
// ============================================================================

@Event("test.event")
data class TestEvent(
    val message: String,
)

@Event("test.another.event")
data class AnotherTestEvent(
    val value: Int,
)

@Configuration
class SingleCustomizerConfig {
    var called = false

    @Bean
    fun customizer() =
        OrbitCustomizer {
            called = true
        }
}

@Configuration
class OrderedCustomizersConfig {
    val callOrder = mutableListOf<String>()

    @Bean
    @Order(1)
    fun firstCustomizer() =
        OrbitCustomizer {
            callOrder.add("first")
        }

    @Bean
    @Order(2)
    fun secondCustomizer() =
        OrbitCustomizer {
            callOrder.add("second")
        }
}
