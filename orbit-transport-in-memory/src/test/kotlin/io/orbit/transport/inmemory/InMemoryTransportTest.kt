package io.orbit.transport.inmemory

import io.orbit.core.transport.MessageTransportTestContract
import io.orbit.core.transport.TransportFactory

class InMemoryTransportTest : MessageTransportTestContract() {
    override fun createTransportFactory(): TransportFactory = InMemoryTransportFactory()

    override val eventuallyTimeoutMs: Long = 1000L
}
