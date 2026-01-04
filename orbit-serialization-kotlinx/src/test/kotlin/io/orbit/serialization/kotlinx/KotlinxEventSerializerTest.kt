package io.orbit.serialization.kotlinx

import io.orbit.core.serializer.EventSerializer
import io.orbit.core.serializer.EventSerializerTestContract

class KotlinxEventSerializerTest : EventSerializerTestContract() {
    override fun createSerializer(): EventSerializer = KotlinxEventSerializer()
}
