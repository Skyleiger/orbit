package io.orbit.serialization.kotlinx

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.orbit.core.event.EventEnvelope
import io.orbit.core.event.EventMetadata
import kotlin.time.Instant
import kotlinx.serialization.Serializable

class KotlinxEventSerializerTest : StringSpec({
  val serializer = KotlinxEventSerializer()

  "serialize should convert EventEnvelope to valid JSON ByteArray" {
    val result = serializer.serialize(TestFixtures.envelope)
    val jsonString = String(result)

    jsonString.isNotBlank() shouldBeEqual true
  }

  "deserialize should convert JSON ByteArray to EventEnvelope" {
    val jsonBytes = TestFixtures.jsonString.toByteArray()
    val result = serializer.deserialize(jsonBytes, TestEvent::class.java)

    result.event.message shouldBeEqual TestFixtures.envelope.event.message
    result.event.number shouldBeEqual TestFixtures.envelope.event.number
    result.metadata.eventId shouldBeEqual TestFixtures.envelope.metadata.eventId
  }

  "serialization and deserialization should be symmetrical" {
    val serialized = serializer.serialize(TestFixtures.envelope)
    val deserialized = serializer.deserialize(serialized, TestEvent::class.java)

    deserialized.event shouldBeEqual TestFixtures.envelope.event
    deserialized.metadata shouldBeEqual TestFixtures.envelope.metadata
  }
})

@Serializable
data class TestEvent(val message: String, val number: Int)

object TestFixtures {
  private val testTimestamp = Instant.parse("2021-08-04T20:00:00Z")
  
  val envelope = EventEnvelope(
    TestEvent("Test message", 42),
    EventMetadata(
      eventId = "event123",
      eventType = "test.event",
      timestamp = testTimestamp,
      source = "test-source",
      tenant = "test-tenant",
      correlationId = "corr123",
      headers = mapOf("key" to "value")
    )
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