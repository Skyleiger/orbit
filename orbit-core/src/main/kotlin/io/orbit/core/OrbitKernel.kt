package io.orbit.core

import io.orbit.core.handler.EventHandler
import io.orbit.core.serializer.EventSerializer
import io.orbit.core.transport.MessageTransport

class OrbitKernel(
  private val serviceName: String,
  private val serializer: EventSerializer,
  private val transport: MessageTransport,
  private val typeRegistry: EventTypeRegistry,
  private val handlerRegistry: EventHandler
) {

}