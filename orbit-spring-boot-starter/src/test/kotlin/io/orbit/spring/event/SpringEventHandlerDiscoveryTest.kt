package io.orbit.spring.event

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.orbit.spring.annotation.EventHandler
import org.springframework.beans.factory.BeanCreationException
import org.springframework.context.ApplicationContext

/**
 * Test suite for [SpringEventHandlerDiscovery].
 *
 * Tests the discovery mechanism for Spring beans with @EventHandler annotations:
 * - Single and multiple handlers per bean
 * - Multiple beans with handlers for the same event
 * - Regular and suspend handler functions
 * - Error cases (missing parameters, invalid beans)
 */
class SpringEventHandlerDiscoveryTest :
    FunSpec({

        context("Handler Discovery") {
            test("should discover single handler in bean") {
                val applicationContext = mockk<ApplicationContext>()
                val handlerBean = SingleHandlerBean()

                every { applicationContext.beanDefinitionNames } returns arrayOf("handler")
                every { applicationContext.getType("handler") } returns SingleHandlerBean::class.java
                every { applicationContext.getBean("handler") } returns handlerBean

                val discovery = SpringEventHandlerDiscovery(applicationContext)
                val result = discovery.discoverEventHandlers()

                result.keys shouldContainExactlyInAnyOrder setOf(DiscoveryTestEvent::class)
                result[DiscoveryTestEvent::class]!! shouldHaveSize 1
            }

            test("should discover multiple handlers in same bean") {
                val applicationContext = mockk<ApplicationContext>()
                val handlerBean = MultipleHandlersBean()

                every { applicationContext.beanDefinitionNames } returns arrayOf("handler")
                every { applicationContext.getType("handler") } returns MultipleHandlersBean::class.java
                every { applicationContext.getBean("handler") } returns handlerBean

                val discovery = SpringEventHandlerDiscovery(applicationContext)
                val result = discovery.discoverEventHandlers()

                result.keys shouldContainExactlyInAnyOrder
                    setOf(
                        DiscoveryTestEvent::class,
                        DiscoveryAnotherEvent::class,
                    )
                result[DiscoveryTestEvent::class]!! shouldHaveSize 1
                result[DiscoveryAnotherEvent::class]!! shouldHaveSize 1
            }

            test("should discover handlers in multiple beans for same event") {
                val applicationContext = mockk<ApplicationContext>()
                val handler1 = SingleHandlerBean()
                val handler2 = AnotherSingleHandlerBean()

                every { applicationContext.beanDefinitionNames } returns arrayOf("handler1", "handler2")
                every { applicationContext.getType("handler1") } returns SingleHandlerBean::class.java
                every { applicationContext.getType("handler2") } returns AnotherSingleHandlerBean::class.java
                every { applicationContext.getBean("handler1") } returns handler1
                every { applicationContext.getBean("handler2") } returns handler2

                val discovery = SpringEventHandlerDiscovery(applicationContext)
                val result = discovery.discoverEventHandlers()

                result.keys shouldContainExactlyInAnyOrder setOf(DiscoveryTestEvent::class)
                result[DiscoveryTestEvent::class]!! shouldHaveSize 2
            }

            test("should discover suspend handler functions") {
                val applicationContext = mockk<ApplicationContext>()
                val handlerBean = SuspendHandlerBean()

                every { applicationContext.beanDefinitionNames } returns arrayOf("handler")
                every { applicationContext.getType("handler") } returns SuspendHandlerBean::class.java
                every { applicationContext.getBean("handler") } returns handlerBean

                val discovery = SpringEventHandlerDiscovery(applicationContext)
                val result = discovery.discoverEventHandlers()

                result.keys shouldContainExactlyInAnyOrder setOf(DiscoveryTestEvent::class)
                result[DiscoveryTestEvent::class]!! shouldHaveSize 1
            }
        }

        context("Bean Filtering") {
            test("should ignore beans without @EventHandler methods") {
                val applicationContext = mockk<ApplicationContext>()

                every { applicationContext.beanDefinitionNames } returns arrayOf("bean")
                every { applicationContext.getType("bean") } returns NoHandlerBean::class.java
                every { applicationContext.getBean("bean") } returns NoHandlerBean()

                val discovery = SpringEventHandlerDiscovery(applicationContext)
                val result = discovery.discoverEventHandlers()

                result shouldBe emptyMap()
            }

            test("should ignore beans that fail to load (BeansException)") {
                val applicationContext = mockk<ApplicationContext>()

                every { applicationContext.beanDefinitionNames } returns arrayOf("failingBean")
                every { applicationContext.getType("failingBean") } returns SingleHandlerBean::class.java
                every { applicationContext.getBean("failingBean") } throws BeanCreationException("Bean creation failed")

                val discovery = SpringEventHandlerDiscovery(applicationContext)
                val result = discovery.discoverEventHandlers()

                result shouldBe emptyMap()
            }

            test("should ignore bean definitions without type") {
                val applicationContext = mockk<ApplicationContext>()

                every { applicationContext.beanDefinitionNames } returns arrayOf("unknownBean")
                every { applicationContext.getType("unknownBean") } returns null

                val discovery = SpringEventHandlerDiscovery(applicationContext)
                val result = discovery.discoverEventHandlers()

                result shouldBe emptyMap()
            }
        }

        context("Error Handling") {
            test("should fail when handler method has no parameters") {
                val applicationContext = mockk<ApplicationContext>()

                every { applicationContext.beanDefinitionNames } returns arrayOf("invalid")
                every { applicationContext.getType("invalid") } returns InvalidNoParamHandlerBean::class.java
                every { applicationContext.getBean("invalid") } returns InvalidNoParamHandlerBean()

                val discovery = SpringEventHandlerDiscovery(applicationContext)

                shouldThrow<IllegalStateException> {
                    discovery.discoverEventHandlers()
                }.message shouldBe "@EventHandler method handle must have an event parameter"
            }
        }
    })

// ============================================================================
// Test Events
// ============================================================================

data class DiscoveryTestEvent(
    val id: String,
)

data class DiscoveryAnotherEvent(
    val value: Int,
)

// ============================================================================
// Test Beans
// ============================================================================

class SingleHandlerBean {
    @Suppress("unused") // invoked via reflection
    @EventHandler
    fun handle(event: DiscoveryTestEvent) {
    }
}

class AnotherSingleHandlerBean {
    @Suppress("unused") // invoked via reflection
    @EventHandler
    fun handle(event: DiscoveryTestEvent) {
    }
}

class MultipleHandlersBean {
    @Suppress("unused") // invoked via reflection
    @EventHandler
    fun handleTest(event: DiscoveryTestEvent) {
    }

    @Suppress("unused") // invoked via reflection
    @EventHandler
    fun handleAnother(event: DiscoveryAnotherEvent) {
    }
}

class SuspendHandlerBean {
    @Suppress("unused", "RedundantSuspendModifier") // invoked via reflection
    @EventHandler
    suspend fun handle(event: DiscoveryTestEvent) {
    }
}

class NoHandlerBean {
    @Suppress("unused") // invoked via reflection
    fun handle(event: DiscoveryTestEvent) {
    }
}

class InvalidNoParamHandlerBean {
    @Suppress("unused") // invoked via reflection
    @EventHandler
    fun handle() {
    }
}
