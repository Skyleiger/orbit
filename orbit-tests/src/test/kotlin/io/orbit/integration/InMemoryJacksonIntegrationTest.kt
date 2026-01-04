package io.orbit.integration

import io.orbit.core.serializer.SerializerFactory
import io.orbit.core.transport.TransportFactory
import io.orbit.serialization.jackson.JacksonSerializerFactory
import io.orbit.transport.inmemory.InMemoryTransportFactory

class InMemoryJacksonIntegrationTest : IntegrationTestContract() {
    override fun createTransportFactory(): TransportFactory = InMemoryTransportFactory()

    override fun createSerializerFactory(): SerializerFactory = JacksonSerializerFactory()

    override val eventuallyTimeoutMs: Long = 1000L
}
