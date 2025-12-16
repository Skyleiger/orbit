package io.orbit.transport.inmemory

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.orbit.core.service.ServiceIdentity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.concurrent.atomics.AtomicInt

/**
 * Tests for InMemoryTransport thread safety and concurrent operations.
 *
 * This test class contains aggressive concurrency tests that verify the implementation
 * is thread-safe when multiple threads access a shared mutable state simultaneously.
 */
class InMemoryTransportConcurrencyTest :
    FunSpec({
        context("Thread Safety") {
            test("should handle concurrent subscriptions") {
                val factory = InMemoryTransportFactory()

                runConcurrentTest { coroutineId, iteration ->
                    val serviceName = "service-$coroutineId"
                    val transport = factory.create(ServiceIdentity(serviceName))
                    transport.connect()
                    val eventType = TestFactory.createEventType("event-$iteration")
                    transport.subscribe(eventType, TestFactory.createHandler {})
                    transport.disconnect()
                }
            }

            test("should handle concurrent unsubscriptions") {
                val factory = InMemoryTransportFactory()
                val transport = factory.create(TestFactory.createServiceIdentity())
                transport.connect()

                // Pre-subscribe to many event types
                val eventTypes = (1..100).map { TestFactory.createEventType("event-$it") }
                eventTypes.forEach { transport.subscribe(it, TestFactory.createHandler {}) }

                runConcurrentTest(coroutineCount = 50, iterationsPerCoroutine = 2) { coroutineId, _ ->
                    val eventType = eventTypes[coroutineId % eventTypes.size]
                    transport.unsubscribe(eventType)
                }

                transport.disconnect()
            }

            test("should handle concurrent message sending") {
                val factory = InMemoryTransportFactory()
                val sender = factory.create(TestFactory.createServiceIdentity("sender"))
                val receiver = factory.create(TestFactory.createServiceIdentity("receiver"))

                sender.connect()
                receiver.connect()

                val eventType = TestFactory.createEventType()
                val receivedCount = AtomicInt(0)
                receiver.subscribe(eventType, TestFactory.createHandler { receivedCount.addAndFetch(1) })

                runConcurrentTest { coroutineId, iteration ->
                    sender.send(TestFactory.createMessage(id = "msg-$coroutineId-$iteration"))
                }

                sender.disconnect()
                receiver.disconnect()

                // All messages should have been received
                receivedCount.load() shouldBe 100 * 50 * 10
            }

            test("should handle concurrent connect operations") {
                val factory = InMemoryTransportFactory()

                runConcurrentTest { coroutineId, _ ->
                    val transport = factory.create(ServiceIdentity("service-$coroutineId"))
                    transport.connect()
                    transport.disconnect()
                }
            }

            test("should handle concurrent disconnect operations") {
                val factory = InMemoryTransportFactory()

                runConcurrentTest { coroutineId, _ ->
                    val transport = factory.create(ServiceIdentity("service-$coroutineId"))
                    transport.connect()
                    transport.disconnect()
                }
            }

            test("should handle concurrent mixed operations") {
                val factory = InMemoryTransportFactory()
                val sender = factory.create(TestFactory.createServiceIdentity("sender"))
                sender.connect()

                runConcurrentTest { coroutineId, iteration ->
                    when (iteration % 4) {
                        0 -> {
                            // Subscribe
                            val transport = factory.create(ServiceIdentity("service-$coroutineId"))
                            transport.connect()
                            val eventType = TestFactory.createEventType("event-$iteration")
                            transport.subscribe(eventType, TestFactory.createHandler {})
                            transport.disconnect()
                        }

                        1 -> {
                            // Send
                            sender.send(TestFactory.createMessage(id = "msg-$coroutineId-$iteration"))
                        }

                        2 -> {
                            // Connect/Disconnect
                            val transport = factory.create(ServiceIdentity("temp-$coroutineId-$iteration"))
                            transport.connect()
                            transport.disconnect()
                        }

                        3 -> {
                            // Subscribe and immediately unsubscribe
                            val transport = factory.create(ServiceIdentity("service-$coroutineId"))
                            transport.connect()
                            val eventType = TestFactory.createEventType("event-$iteration")
                            transport.subscribe(eventType, TestFactory.createHandler {})
                            transport.unsubscribe(eventType)
                            transport.disconnect()
                        }
                    }
                }

                sender.disconnect()
            }
        }
    })

/**
 * Executes a highly concurrent test with the given operation.
 * Uses a barrier to ensure all coroutines start simultaneously, maximizing contention.
 */
private suspend fun runConcurrentTest(
    coroutineCount: Int = 100,
    iterationsPerCoroutine: Int = 50,
    repetitions: Int = 10,
    operation: suspend (coroutineId: Int, iteration: Int) -> Unit,
) {
    repeat(repetitions) { _ ->
        val barrier = CompletableDeferred<Unit>()
        val successCount = AtomicInt(0)
        val errorCount = AtomicInt(0)
        var firstError: Exception? = null
        val errorMutex = Mutex()

        withContext(Dispatchers.Default) {
            coroutineScope {
                repeat(coroutineCount) { coroutineId ->
                    launch {
                        barrier.await()
                        repeat(iterationsPerCoroutine) { iteration ->
                            try {
                                operation(coroutineId, iteration)
                                successCount.addAndFetch(1)
                            } catch (e: Exception) {
                                errorMutex.withLock {
                                    if (firstError == null) firstError = e
                                }
                                errorCount.addAndFetch(1)
                            }
                        }
                    }
                }
                barrier.complete(Unit)
            }
        }

        val expectedTotal = coroutineCount * iterationsPerCoroutine
        val actualTotal = successCount.load() + errorCount.load()
        actualTotal shouldBe expectedTotal
        if (errorCount.load() > 0) {
            throw AssertionError(
                "Expected 0 errors but got ${errorCount.load()}. First error: $firstError",
                firstError,
            )
        }
    }
}
