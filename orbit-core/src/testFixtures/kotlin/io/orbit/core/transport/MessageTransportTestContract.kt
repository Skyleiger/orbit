package io.orbit.core.transport

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.orbit.core.event.EventType
import io.orbit.core.service.ServiceIdentity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.concurrent.atomics.AtomicInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

/**
 * Abstract contract test suite for MessageTransport implementations.
 *
 * All MessageTransport implementations must pass these tests to ensure they
 * correctly implement the MessageTransport interface contract.
 *
 * The tests are organized into focused categories:
 * - Connection Management: connect/disconnect lifecycle
 * - Basic Message Delivery: send/receive messages
 * - Subscription Management: subscribe/unsubscribe operations
 * - Error Handling: error scenarios
 * - Message Distribution: multi-instance behavior
 *
 * Implementations can add their own tests by adding a second `init` block:
 * ```kotlin
 * class MyTransportTest : MessageTransportTestContract() {
 *     override fun createTransportFactory() = MyTransportFactory()
 *
 *     init {
 *         context("MyTransport-Specific Features") {
 *             // unique transport context
 *             test("should support feature X") {
 *                 val transport = createTransport()
 *
 *                 // ... test implementation-specific behavior
 *             }
 *
 *             // same transport context, but unique service name
 *             test("should support feature Y") {
 *                 val transportFactory = createTransportFactory()
 *                 val transport = createTransport(transportFactory)
 *
 *                 // ... test implementation-specific behavior
 *             }
 *
 *             // same transport context, same service name
 *             test("should support feature Z") {
 *                 val transportFactory = createTransportFactory()
 *                 val serviceName = generateServiceName()
 *                 val transport =
 *                   createTransport(transportFactory, serviceName)
 *                 val transport2 =
 *                   createTransport(transportFactory, serviceName)
 *
 *                 // ... test implementation-specific behavior
 *             }
 *         }
 *     }
 * }
 * ```
 */
abstract class MessageTransportTestContract : FunSpec() {
    /**
     * Provided the TransportFactory instance for creating transports for testing.
     * Must be implemented by each transport implementation.
     */
    abstract fun createTransportFactory(): TransportFactory

    /**
     * Timeout in milliseconds for eventual consistency checks.
     * Override based on transport characteristics (network-based needs longer).
     */
    open val eventuallyTimeoutMs: Long = 5000L

    init {
        // region Connection Management

        context("Connection Management") {
            test("should start disconnected") {
                val transport = createTransport()
                transport.isConnected() shouldBe false
            }

            test("should connect successfully") {
                val transport = createTransport()
                transport.connect()
                transport.isConnected() shouldBe true
                transport.disconnect()
            }

            test("should disconnect successfully") {
                val transport = createTransport()
                transport.connect()
                transport.disconnect()
                transport.isConnected() shouldBe false
            }

            test("should allow reconnection after disconnect") {
                val transport = createTransport()
                transport.connect()
                transport.disconnect()
                transport.connect()
                transport.isConnected() shouldBe true
                transport.disconnect()
            }

            test("should handle multiple connect calls") {
                val transport = createTransport()
                transport.connect()
                transport.connect()
                transport.connect()
                transport.isConnected() shouldBe true
                transport.disconnect()
            }

            test("should handle multiple disconnect calls") {
                val transport = createTransport()
                transport.connect()
                transport.disconnect()
                transport.disconnect()
                transport.disconnect()
                transport.isConnected() shouldBe false
            }

            test("should handle disconnect without connect") {
                val transport = createTransport()
                transport.disconnect()
                transport.isConnected() shouldBe false
            }
        }

        // endregion

        // region Basic Message Delivery

        context("Basic Message Delivery") {
            test("should send and receive message") {
                val transport = createTransport()
                transport.connect()

                val eventType = createEventType()
                val receivedMessages = mutableListOf<TransportMessage>()

                transport.subscribe(eventType, createHandler { receivedMessages.add(it) })

                val message = createMessage()
                transport.send(message)

                eventually(eventuallyTimeoutMs.milliseconds) {
                    receivedMessages.size shouldBe 1
                    receivedMessages[0].messageId shouldBe message.messageId
                    receivedMessages[0].eventType shouldBe message.eventType
                }

                transport.disconnect()
            }

            test("should not deliver message to unsubscribed event type") {
                val transport = createTransport()
                transport.connect()

                val subscribedEventType = createEventType("subscribed.event")
                val unsubscribedEventType = createEventType("unsubscribed.event")
                val receivedMessages = mutableListOf<TransportMessage>()

                transport.subscribe(subscribedEventType, createHandler { receivedMessages.add(it) })

                val message = createMessage(eventType = unsubscribedEventType.value)
                transport.send(message)

                delay(eventuallyTimeoutMs)
                receivedMessages.size shouldBe 0

                transport.disconnect()
            }

            test("should handle multiple event types independently") {
                val transport = createTransport()
                transport.connect()

                val eventType1 = createEventType("event.type1")
                val eventType2 = createEventType("event.type2")

                val messages1 = mutableListOf<TransportMessage>()
                val messages2 = mutableListOf<TransportMessage>()

                transport.subscribe(eventType1, createHandler { messages1.add(it) })
                transport.subscribe(eventType2, createHandler { messages2.add(it) })

                val message1 = createMessage(id = "msg-1", eventType = eventType1.value)
                val message2 = createMessage(id = "msg-2", eventType = eventType2.value)

                transport.send(message1)
                transport.send(message2)

                eventually(eventuallyTimeoutMs.milliseconds) {
                    messages1.size shouldBe 1
                    messages2.size shouldBe 1
                    messages1[0].messageId shouldBe "msg-1"
                    messages2[0].messageId shouldBe "msg-2"
                }

                transport.disconnect()
            }
        }

        // endregion

        // region Subscription Management

        context("Subscription Management") {
            test("should unsubscribe from event type") {
                val transport = createTransport()
                transport.connect()

                val eventType = createEventType()
                val receivedMessages = mutableListOf<TransportMessage>()

                transport.subscribe(eventType, createHandler { receivedMessages.add(it) })
                transport.unsubscribe(eventType)

                transport.send(createMessage())
                delay(eventuallyTimeoutMs)

                receivedMessages.size shouldBe 0

                transport.disconnect()
            }

            test("should cleanup all subscriptions on disconnect") {
                val transport = createTransport()
                transport.connect()

                val eventType = createEventType()
                var messageCount = 0

                transport.subscribe(eventType, createHandler { messageCount++ })
                transport.disconnect()

                transport.connect()
                transport.send(createMessage())
                delay(eventuallyTimeoutMs)

                messageCount shouldBe 0

                transport.disconnect()
            }

            test("should handle multiple subscriptions on same transport") {
                val transport = createTransport()
                transport.connect()

                val eventType1 = createEventType("event1")
                val eventType2 = createEventType("event2")
                val eventType3 = createEventType("event3")

                var count1 = 0
                var count2 = 0
                var count3 = 0

                transport.subscribe(eventType1, createHandler { count1++ })
                transport.subscribe(eventType2, createHandler { count2++ })
                transport.subscribe(eventType3, createHandler { count3++ })

                transport.send(createMessage(eventType = eventType1.value))
                transport.send(createMessage(eventType = eventType2.value))
                transport.send(createMessage(eventType = eventType3.value))

                eventually(eventuallyTimeoutMs.milliseconds) {
                    count1 shouldBe 1
                    count2 shouldBe 1
                    count3 shouldBe 1
                }

                transport.disconnect()
            }

            test("should handle unsubscribe of non-existent subscription") {
                val transport = createTransport()
                transport.connect()

                val eventType = createEventType()
                transport.unsubscribe(eventType)

                transport.disconnect()
            }
        }

        // endregion

        // region Error Handling

        context("Error Handling") {
            test("should throw when sending without connection") {
                val transport = createTransport()
                val message = createMessage()

                val exception =
                    shouldThrow<IllegalStateException> {
                        transport.send(message)
                    }
                exception.message shouldContain "not connected"
            }

            test("should throw when subscribing without connection") {
                val transport = createTransport()
                val eventType = createEventType()

                val exception =
                    shouldThrow<IllegalStateException> {
                        transport.subscribe(eventType, createHandler {})
                    }
                exception.message shouldContain "not connected"
            }

            test("should throw when unsubscribing without connection") {
                val transport = createTransport()
                val eventType = createEventType()

                val exception =
                    shouldThrow<IllegalStateException> {
                        transport.unsubscribe(eventType)
                    }
                exception.message shouldContain "not connected"
            }
        }

        // endregion

        // region Message Distribution

        context("Message Distribution") {
            test("should distribute messages to multiple services") {
                val transportFactory = createTransportFactory()
                val transport1 = createTransport(transportFactory)
                val transport2 = createTransport(transportFactory)

                transport1.connect()
                transport2.connect()

                val eventType = createEventType()
                var count1 = 0
                var count2 = 0

                transport1.subscribe(eventType, createHandler { count1++ })
                transport2.subscribe(eventType, createHandler { count2++ })

                transport1.send(createMessage())

                eventually(eventuallyTimeoutMs.milliseconds) {
                    count1 shouldBe 1
                    count2 shouldBe 1
                }

                transport1.disconnect()
                transport2.disconnect()
            }

            test("should load balance messages across same service instances") {
                val transportFactory = createTransportFactory()
                val serviceName = generateServiceName()
                val instance1 = createTransport(transportFactory, serviceName)
                val instance2 = createTransport(transportFactory, serviceName)

                instance1.connect()
                instance2.connect()

                val eventType = createEventType()
                var count1 = 0
                var count2 = 0

                instance1.subscribe(eventType, createHandler { count1++ })
                instance2.subscribe(eventType, createHandler { count2++ })

                val messageCount = 10
                repeat(messageCount) { i ->
                    instance1.send(createMessage(id = "msg-$i"))
                }

                eventually(eventuallyTimeoutMs.milliseconds) {
                    count1 + count2 shouldBe messageCount
                }

                count1 shouldBeGreaterThan 0
                count2 shouldBeGreaterThan 0

                instance1.disconnect()
                instance2.disconnect()
            }
        }

        // endregion

        // region Concurrency & Thread Safety

        context("Concurrency") {
            test("should handle concurrent subscriptions") {
                val transportFactory = createTransportFactory()

                runConcurrentTest(
                    coroutineCount = 20,
                    iterationsPerCoroutine = 5,
                    repetitions = 3,
                ) { coroutineId, iteration ->
                    val serviceName = "service-$coroutineId"
                    val transport = createTransport(transportFactory, serviceName)
                    transport.connect()
                    val eventType = createEventType("event-$iteration")
                    transport.subscribe(eventType, createHandler {})
                    transport.disconnect()
                }
            }

            test("should handle concurrent unsubscriptions") {
                val transportFactory = createTransportFactory()
                val transport = createTransport(transportFactory)
                transport.connect()

                // Pre-subscribe to many event types
                val eventTypes = (1..20).map { createEventType("event-$it") }
                eventTypes.forEach { transport.subscribe(it, createHandler {}) }

                runConcurrentTest(
                    coroutineCount = 10,
                    iterationsPerCoroutine = 2,
                    repetitions = 2,
                ) { coroutineId, _ ->
                    val eventType = eventTypes[coroutineId % eventTypes.size]
                    transport.unsubscribe(eventType)
                }

                transport.disconnect()
            }

            test("should handle concurrent message sending") {
                val transportFactory = createTransportFactory()
                val sender = createTransport(transportFactory, "sender")
                val receiver = createTransport(transportFactory, "receiver")

                sender.connect()
                receiver.connect()

                val eventType = createEventType()
                val receivedCount = AtomicInt(0)
                receiver.subscribe(eventType, createHandler { receivedCount.addAndFetch(1) })

                val concurrencyCount = 10
                val iterCount = 5
                val reps = 2
                runConcurrentTest(
                    coroutineCount = concurrencyCount,
                    iterationsPerCoroutine = iterCount,
                    repetitions = reps,
                ) { coroutineId, iteration ->
                    sender.send(createMessage(id = "msg-$coroutineId-$iteration"))
                }

                eventually(eventuallyTimeoutMs.milliseconds) {
                    receivedCount.load() shouldBe concurrencyCount * iterCount * reps
                }

                sender.disconnect()
                receiver.disconnect()
            }

            test("should handle concurrent connect operations") {
                val transportFactory = createTransportFactory()

                runConcurrentTest(
                    coroutineCount = 20,
                    iterationsPerCoroutine = 3,
                    repetitions = 2,
                ) { coroutineId, iteration ->
                    val transport = createTransport(transportFactory, "service-$coroutineId-$iteration")
                    transport.connect()
                    transport.disconnect()
                }
            }

            test("should handle concurrent disconnect operations") {
                val transportFactory = createTransportFactory()

                runConcurrentTest(
                    coroutineCount = 20,
                    iterationsPerCoroutine = 3,
                    repetitions = 2,
                ) { coroutineId, iteration ->
                    val transport = createTransport(transportFactory, "service-$coroutineId-$iteration")
                    transport.connect()
                    transport.disconnect()
                }
            }

            test("should handle concurrent mixed operations") {
                val transportFactory = createTransportFactory()
                val sender = createTransport(transportFactory, "sender")
                sender.connect()

                runConcurrentTest(
                    coroutineCount = 20,
                    iterationsPerCoroutine = 4,
                    repetitions = 2,
                ) { coroutineId, iteration ->
                    when (iteration % 4) {
                        0 -> {
                            // Subscribe
                            val transport = createTransport(transportFactory, "service-$coroutineId")
                            transport.connect()
                            val eventType = createEventType("event-$iteration")
                            transport.subscribe(eventType, createHandler {})
                            transport.disconnect()
                        }

                        1 -> {
                            // Send
                            sender.send(createMessage(id = "msg-$coroutineId-$iteration"))
                        }

                        2 -> {
                            // Connect/Disconnect
                            val transport = createTransport(transportFactory, "temp-$coroutineId-$iteration")
                            transport.connect()
                            transport.disconnect()
                        }

                        3 -> {
                            // Subscribe and immediately unsubscribe
                            val transport = createTransport(transportFactory, "service-$coroutineId")
                            transport.connect()
                            val eventType = createEventType("event-$iteration")
                            transport.subscribe(eventType, createHandler {})
                            transport.unsubscribe(eventType)
                            transport.disconnect()
                        }
                    }
                }

                sender.disconnect()
            }
        }

        // endregion
    }

    // region Test Helpers (protected for use in subclasses)

    /**
     * Creates a message transport with a generated service name.
     */
    protected fun createTransport(
        transportFactory: TransportFactory = createTransportFactory(),
        serviceName: String = generateServiceName(),
    ): MessageTransport = transportFactory.create(ServiceIdentity(serviceName))

    /**
     * Generates a random service name for testing.
     */
    protected fun generateServiceName(): String = "test-service-${Uuid.random()}"

    /**
     * Creates a test message with the given parameters.
     */
    protected fun createMessage(
        id: String = "test-msg-1",
        eventType: String = "test.event",
        body: String = "test body",
    ) = TransportMessage(
        messageId = id,
        eventType = eventType,
        contentType = "application/json",
        contentEncoding = "utf-8",
        body = body.toByteArray(),
    )

    /**
     * Creates a test event type.
     */
    protected fun createEventType(name: String = "test.event") = EventType(name)

    /**
     * Creates a test message handler.
     */
    protected fun createHandler(onMessage: (TransportMessage) -> Unit) = MessageHandler { onMessage(it) }

    /**
     * Executes a highly concurrent test with the given operation.
     * Uses a barrier to ensure all coroutines start simultaneously, maximizing contention.
     */
    protected suspend fun runConcurrentTest(
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

    // endregion
}
