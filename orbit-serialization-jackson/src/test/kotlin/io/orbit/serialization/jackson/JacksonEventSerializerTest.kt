package io.orbit.serialization.jackson

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.orbit.core.event.EventEnvelope
import io.orbit.core.event.EventMetadata
import kotlin.time.Instant

class JacksonEventSerializerTest :
    StringSpec({
        val serializer = JacksonEventSerializer()

        "serialize should convert EventEnvelope to valid JSON ByteArray" {
            val result = serializer.serialize(TestFixtures.envelope)
            val jsonString = String(result)

            jsonString shouldEqualJson TestFixtures.jsonString
        }

        "deserialize should convert JSON ByteArray to EventEnvelope" {
            val jsonBytes = TestFixtures.jsonString.toByteArray()
            val result = serializer.deserialize(jsonBytes, TestEvent::class.java)

            result shouldBeEqual TestFixtures.envelope
        }

        "serialization and deserialization should be symmetrical" {
            val serialized = serializer.serialize(TestFixtures.envelope)
            val deserialized = serializer.deserialize(serialized, TestEvent::class.java)

            deserialized shouldBeEqual TestFixtures.envelope
        }
    })

private data class TestEvent(
    val message: String,
    val number: Int,
)

private object TestFixtures {
    private val testTimestamp = Instant.parse("2021-08-04T20:00:00Z")

    val envelope =
        EventEnvelope(
            TestEvent("Test message", 42),
            EventMetadata(
                eventId = "event123",
                eventType = "test.event",
                timestamp = testTimestamp,
                source = "test-source",
                tenant = "test-tenant",
                correlationId = "corr123",
                headers = mapOf("key" to "value"),
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
            "timestamp": "2021-08-04T20:00:00Z",
            "source": "test-source",
            "tenant": "test-tenant",
            "correlationId": "corr123",
            "headers": {
              "key": "value"
            }
          }
        }
        """.trimIndent()
}
