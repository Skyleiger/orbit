package io.orbit.serialization.jackson

import io.orbit.core.serializer.EventSerializer
import io.orbit.core.serializer.EventSerializerTestContract

class JacksonEventSerializerTest : EventSerializerTestContract() {
    override fun createSerializer(): EventSerializer = JacksonEventSerializer()
}
