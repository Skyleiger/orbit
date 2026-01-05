package io.orbit.spring.event

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.reflect.jvm.kotlinFunction

class SpringEventHandlerAdapterTest :
    FunSpec({

        context("JavaEventHandlerAdapter") {
            test("should invoke method on bean") {
                val handler = TestJavaHandler()
                val method = handler::class.java.getMethod("handleEvent", TestEvent::class.java)
                val adapter = JavaEventHandlerAdapter<TestEvent>(handler, method)

                adapter.handle(TestEvent("message-1"))
                adapter.handle(TestEvent("message-2"))
                adapter.handle(TestEvent("message-3"))

                handler.receivedEvents.size shouldBe 3
                handler.receivedEvents.map { it.message } shouldBe listOf("message-1", "message-2", "message-3")
            }
        }

        context("KotlinEventHandlerAdapter") {
            test("should invoke normal function on bean") {
                val handler = TestKotlinHandler()
                val method = handler::class.java.methods.first { it.name == "handleEvent" }
                val kotlinFunction = method.kotlinFunction!!
                val adapter = KotlinEventHandlerAdapter<TestEvent>(handler, kotlinFunction)

                adapter.handle(TestEvent("kotlin-1"))
                adapter.handle(TestEvent("kotlin-2"))

                handler.receivedEvents.size shouldBe 2
                handler.receivedEvents.map { it.message } shouldBe listOf("kotlin-1", "kotlin-2")
            }

            test("should invoke suspend function on bean") {
                val handler = TestKotlinSuspendHandler()
                val method = handler::class.java.methods.first { it.name == "handleEvent" }
                val kotlinFunction = method.kotlinFunction!!
                val adapter = KotlinEventHandlerAdapter<TestEvent>(handler, kotlinFunction)

                adapter.handle(TestEvent("kotlin-suspend-1"))
                adapter.handle(TestEvent("kotlin-suspend-2"))
                adapter.handle(TestEvent("kotlin-suspend-3"))

                handler.receivedEvents.size shouldBe 3
                handler.receivedEvents.map { it.message } shouldBe
                    listOf(
                        "kotlin-suspend-1",
                        "kotlin-suspend-2",
                        "kotlin-suspend-3",
                    )
            }
        }
    })

data class TestEvent(
    val message: String,
)

class TestJavaHandler {
    val receivedEvents = mutableListOf<TestEvent>()

    @Suppress("unused") // invoked via reflection
    fun handleEvent(event: TestEvent) {
        receivedEvents.add(event)
    }
}

class TestKotlinHandler {
    val receivedEvents = mutableListOf<TestEvent>()

    @Suppress("unused") // invoked via reflection
    fun handleEvent(event: TestEvent) {
        receivedEvents.add(event)
    }
}

class TestKotlinSuspendHandler {
    val receivedEvents = mutableListOf<TestEvent>()

    @Suppress("unused") // invoked via reflection
    suspend fun handleEvent(event: TestEvent) {
        delay(1L) // Simulate some async work
        receivedEvents.add(event)
    }
}
