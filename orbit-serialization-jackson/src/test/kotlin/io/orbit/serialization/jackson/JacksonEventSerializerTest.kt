package io.orbit.serialization.jackson

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.orbit.core.event.EventEnvelope
import io.orbit.core.event.EventMetadata
import io.orbit.core.serializer.SerializedEvent
import kotlin.time.Instant

class JacksonEventSerializerTest :
    StringSpec({
        val serializer = JacksonEventSerializer()

        "serialize should convert EventEnvelope to valid JSON ByteArray" {
            val result = serializer.serialize(TestFixtures.envelope)
            val jsonString = String(result.data)

            jsonString shouldEqualJson TestFixtures.jsonString
            result.contentType shouldBe "application/json"
            result.contentEncoding shouldBe "utf-8"
        }

        "deserialize should convert JSON ByteArray to EventEnvelope" {
            val serialized =
                SerializedEvent(
                    data = TestFixtures.jsonString.toByteArray(),
                    contentType = "application/json",
                    contentEncoding = "utf-8",
                )
            val result = serializer.deserialize(serialized, TestEvent::class.java)

            result shouldBeEqual TestFixtures.envelope
        }

        "serialization and deserialization should be symmetrical" {
            val serialized = serializer.serialize(TestFixtures.envelope)
            val deserialized = serializer.deserialize(serialized, TestEvent::class.java)

            deserialized shouldBeEqual TestFixtures.envelope
        }

        "deserialize should throw exception for unsupported content type" {
            val serialized =
                SerializedEvent(
                    data = TestFixtures.jsonString.toByteArray(),
                    contentType = "application/protobuf",
                    contentEncoding = "utf-8",
                )

            val exception =
                runCatching { serializer.deserialize(serialized, TestEvent::class.java) }
                    .exceptionOrNull()

            exception shouldBe
                io.kotest.matchers.types
                    .instanceOf<IllegalArgumentException>()
            exception?.message shouldBe "Unsupported content type: application/protobuf. Expected: application/json"
        }

        "deserialize should throw exception for unsupported content encoding" {
            val serialized =
                SerializedEvent(
                    data = TestFixtures.jsonString.toByteArray(),
                    contentType = "application/json",
                    contentEncoding = "gzip",
                )

            val exception =
                runCatching { serializer.deserialize(serialized, TestEvent::class.java) }
                    .exceptionOrNull()

            exception shouldBe
                io.kotest.matchers.types
                    .instanceOf<IllegalArgumentException>()
            exception?.message shouldBe "Unsupported content encoding: gzip. Expected: utf-8"
        }
    })

private data class TestEvent(
    val message: String,
    val number: Int,
)

private object TestFixtures {
    val envelope =
        EventEnvelope(
            TestEvent("Test message", 42),
            EventMetadata(
                eventId = "event123",
                eventType = "test.event",
                timestamp = Instant.parse("2024-01-15T10:00:00Z"),
                source = "test-source",
            ),
        )

    val jsonString =
        """
        {
          "event": {
            "message": "Test message",
            "number": 42
          },
          "metadata": {
            "eventId": "event123",
            "eventType": "test.event",
            "timestamp": "2024-01-15T10:00:00Z",
            "source": "test-source"
          }
        }
        """.trimIndent()
}
