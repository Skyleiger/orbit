package io.orbit.core.serializer

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.orbit.core.event.EventEnvelope
import io.orbit.core.event.EventMetadata
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Abstract contract test suite for EventSerializer implementations.
 *
 * All EventSerializer implementations must pass these tests to ensure they
 * correctly implement the EventSerializer interface contract.
 *
 * The tests verify:
 * - Serialization: converting EventEnvelope to SerializedEvent
 * - Deserialization: converting SerializedEvent back to EventEnvelope
 * - Symmetry: serialize -> deserialize should preserve data
 * - Error Handling: proper handling of unsupported content types/encodings
 *
 * Implementations can add their own tests by adding a second `init` block:
 * ```kotlin
 * class MySerializerTest : EventSerializerTestContract() {
 *     override fun createSerializer() = MySerializer()
 *
 *     init {
 *         context("MySerializer-Specific Features") {
 *             test("should support custom feature") {
 *                 val serializer = createSerializer()
 *                 // ... test implementation-specific behavior
 *             }
 *         }
 *     }
 * }
 * ```
 */
abstract class EventSerializerTestContract : FunSpec() {
    /**
     * Creates the EventSerializer instance for testing.
     * Must be implemented by each serializer implementation.
     */
    abstract fun createSerializer(): EventSerializer

    /**
     * Returns the expected content type for this serializer.
     * Default is "application/json", override if needed.
     */
    open val expectedContentType: String = "application/json"

    /**
     * Returns the expected content encoding for this serializer.
     * Default is "utf-8", override if needed.
     */
    open val expectedContentEncoding: String = "utf-8"

    /**
     * Creates a test event envelope with the given event.
     * Override to customize the test envelope.
     */
    open fun <T : Any> createTestEnvelope(event: T): EventEnvelope<T> =
        EventEnvelope(
            event = event,
            metadata =
                EventMetadata(
                    eventId = "event123",
                    eventType = "test.event",
                    timestamp = Instant.parse("2024-01-15T10:00:00Z"),
                    source = "test-source",
                ),
        )

    init {
        // region Serialization

        context("Serialization") {
            test("should serialize EventEnvelope to SerializedEvent") {
                val serializer = createSerializer()
                val envelope = createTestEnvelope(TestEvent("Test message", 42))

                val result = serializer.serialize(envelope)

                result.data.isNotEmpty() shouldBe true
                result.contentType shouldBe expectedContentType
                result.contentEncoding shouldBe expectedContentEncoding
            }

            test("should serialize different event types") {
                val serializer = createSerializer()
                val stringEnvelope = createTestEnvelope(StringEvent("text"))
                val numberEnvelope = createTestEnvelope(NumberEvent(123))

                val result1 = serializer.serialize(stringEnvelope)
                val result2 = serializer.serialize(numberEnvelope)

                result1.data.isNotEmpty() shouldBe true
                result2.data.isNotEmpty() shouldBe true
                result1.data.contentEquals(result2.data) shouldBe false
            }

            test("should serialize complex event objects") {
                val serializer = createSerializer()
                val complexEvent =
                    ComplexEvent(
                        name = "complex",
                        nested = NestedData("nested value", 99),
                        list = listOf("a", "b", "c"),
                    )
                val envelope = createTestEnvelope(complexEvent)

                val result = serializer.serialize(envelope)

                result.data.isNotEmpty() shouldBe true
                result.contentType shouldBe expectedContentType
            }
        }

        // endregion

        // region Deserialization

        context("Deserialization") {
            test("should deserialize SerializedEvent to EventEnvelope") {
                val serializer = createSerializer()
                val originalEnvelope = createTestEnvelope(TestEvent("Test message", 42))
                val serialized = serializer.serialize(originalEnvelope)

                val result = serializer.deserialize(serialized, TestEvent::class.java)

                result.event.message shouldBeEqual originalEnvelope.event.message
                result.event.number shouldBeEqual originalEnvelope.event.number
                result.metadata.eventId shouldBeEqual originalEnvelope.metadata.eventId
                result.metadata.eventType shouldBeEqual originalEnvelope.metadata.eventType
                result.metadata.source shouldBeEqual originalEnvelope.metadata.source
            }

            test("should deserialize different event types") {
                val serializer = createSerializer()
                val stringEnvelope = createTestEnvelope(StringEvent("text"))
                val serializedString = serializer.serialize(stringEnvelope)

                val result = serializer.deserialize(serializedString, StringEvent::class.java)

                result.event.value shouldBeEqual stringEnvelope.event.value
            }

            test("should deserialize complex event objects") {
                val serializer = createSerializer()
                val complexEvent =
                    ComplexEvent(
                        name = "complex",
                        nested = NestedData("nested value", 99),
                        list = listOf("a", "b", "c"),
                    )
                val originalEnvelope = createTestEnvelope(complexEvent)
                val serialized = serializer.serialize(originalEnvelope)

                val result = serializer.deserialize(serialized, ComplexEvent::class.java)

                result.event.name shouldBeEqual complexEvent.name
                result.event.nested.text shouldBeEqual complexEvent.nested.text
                result.event.nested.count shouldBeEqual complexEvent.nested.count
                result.event.list shouldBeEqual complexEvent.list
            }
        }

        // endregion

        // region Symmetry

        context("Symmetry") {
            test("serialization and deserialization should be symmetrical") {
                val serializer = createSerializer()
                val originalEnvelope = createTestEnvelope(TestEvent("Test message", 42))

                val serialized = serializer.serialize(originalEnvelope)
                val deserialized = serializer.deserialize(serialized, TestEvent::class.java)

                deserialized.event shouldBeEqual originalEnvelope.event
                deserialized.metadata shouldBeEqual originalEnvelope.metadata
            }

            test("multiple serialize-deserialize cycles should preserve data") {
                val serializer = createSerializer()
                var envelope = createTestEnvelope(TestEvent("Cycle test", 999))

                repeat(3) {
                    val serialized = serializer.serialize(envelope)
                    envelope = serializer.deserialize(serialized, TestEvent::class.java)
                }

                envelope.event.message shouldBeEqual "Cycle test"
                envelope.event.number shouldBeEqual 999
            }

            test("should handle empty string values") {
                val serializer = createSerializer()
                val envelope = createTestEnvelope(StringEvent(""))

                val serialized = serializer.serialize(envelope)
                val deserialized = serializer.deserialize(serialized, StringEvent::class.java)

                deserialized.event.value shouldBeEqual ""
            }

            test("should handle zero numeric values") {
                val serializer = createSerializer()
                val envelope = createTestEnvelope(NumberEvent(0))

                val serialized = serializer.serialize(envelope)
                val deserialized = serializer.deserialize(serialized, NumberEvent::class.java)

                deserialized.event.value shouldBeEqual 0
            }

            test("should handle empty lists") {
                val serializer = createSerializer()
                val envelope =
                    createTestEnvelope(
                        ComplexEvent(
                            name = "empty",
                            nested = NestedData("test", 1),
                            list = emptyList(),
                        ),
                    )

                val serialized = serializer.serialize(envelope)
                val deserialized = serializer.deserialize(serialized, ComplexEvent::class.java)

                deserialized.event.list shouldBeEqual emptyList()
            }
        }

        // endregion

        // region Error Handling

        context("Error Handling") {
            test("should throw exception for unsupported content type") {
                val serializer = createSerializer()
                val serialized =
                    SerializedEvent(
                        data = "{}".toByteArray(),
                        contentType = "application/protobuf",
                        contentEncoding = expectedContentEncoding,
                    )

                val exception =
                    shouldThrow<IllegalArgumentException> {
                        serializer.deserialize(serialized, TestEvent::class.java)
                    }

                exception.message shouldContain "Unsupported content type"
                exception.message shouldContain "application/protobuf"
            }

            test("should throw exception for unsupported content encoding") {
                val serializer = createSerializer()
                val serialized =
                    SerializedEvent(
                        data = "{}".toByteArray(),
                        contentType = expectedContentType,
                        contentEncoding = "gzip",
                    )

                val exception =
                    shouldThrow<IllegalArgumentException> {
                        serializer.deserialize(serialized, TestEvent::class.java)
                    }

                exception.message shouldContain "Unsupported content encoding"
                exception.message shouldContain "gzip"
            }

            test("should throw exception for invalid JSON data") {
                val serializer = createSerializer()
                val serialized =
                    SerializedEvent(
                        data = "not valid json".toByteArray(),
                        contentType = expectedContentType,
                        contentEncoding = expectedContentEncoding,
                    )

                shouldThrow<Exception> {
                    serializer.deserialize(serialized, TestEvent::class.java)
                }
            }

            test("should throw exception for malformed event structure") {
                val serializer = createSerializer()
                val serialized =
                    SerializedEvent(
                        data = """{"invalid": "structure"}""".toByteArray(),
                        contentType = expectedContentType,
                        contentEncoding = expectedContentEncoding,
                    )

                shouldThrow<Exception> {
                    serializer.deserialize(serialized, TestEvent::class.java)
                }
            }
        }

        // endregion
    }

    // region Test Event Classes

    @Serializable
    data class TestEvent(
        val message: String,
        val number: Int,
    )

    @Serializable
    data class StringEvent(
        val value: String,
    )

    @Serializable
    data class NumberEvent(
        val value: Int,
    )

    @Serializable
    data class ComplexEvent(
        val name: String,
        val nested: NestedData,
        val list: List<String>,
    )

    @Serializable
    data class NestedData(
        val text: String,
        val count: Int,
    )

    // endregion
}
