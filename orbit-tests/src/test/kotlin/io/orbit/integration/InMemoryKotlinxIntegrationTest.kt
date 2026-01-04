package io.orbit.integration

import io.orbit.core.serializer.SerializerFactory
import io.orbit.core.transport.TransportFactory
import io.orbit.serialization.kotlinx.KotlinxSerializerFactory
import io.orbit.transport.inmemory.InMemoryTransportFactory

class InMemoryKotlinxIntegrationTest : IntegrationTestContract() {
    override fun createTransportFactory(): TransportFactory = InMemoryTransportFactory()

    override fun createSerializerFactory(): SerializerFactory = KotlinxSerializerFactory()

    override val eventuallyTimeoutMs: Long = 1000L
}
