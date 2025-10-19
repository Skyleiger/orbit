package io.orbit.core

import io.orbit.core.event.EventRegistry
import io.orbit.core.handler.EventHandler
import io.orbit.core.serializer.EventSerializer
import io.orbit.core.transport.MessageTransport

class OrbitKernel(
    private val serviceName: String,
    private val serializer: EventSerializer,
    private val transport: MessageTransport,
    private val eventRegistry: EventRegistry,
    private val handlerRegistry: EventHandler,
)
