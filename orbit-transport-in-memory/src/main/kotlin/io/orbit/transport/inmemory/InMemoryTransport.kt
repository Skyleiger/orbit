package io.orbit.transport.inmemory

import io.orbit.core.event.EventType
import io.orbit.core.service.ServiceIdentity
import io.orbit.core.transport.MessageHandler
import io.orbit.core.transport.MessageTransport
import io.orbit.core.transport.TransportMessage

/**
 * In-memory implementation of [MessageTransport] for testing and development.
 *
 * This transport uses an internal message bus to deliver messages between
 * different transport instances. Each [InMemoryTransport] instance maintains
 * its own connection state, but all instances connected to the same message
 * bus can communicate with each other.
 *
 * @param messageBus The shared message bus for inter-transport communication.
 * @param serviceIdentity The identity of the service this transport belongs to.
 */
class InMemoryTransport(
    private val messageBus: InMemoryMessageBus,
    private val serviceIdentity: ServiceIdentity,
) : MessageTransport {
    private var isConnected = false

    override suspend fun connect() {
        isConnected = true
    }

    override suspend fun disconnect() {
        isConnected = false
        messageBus.unsubscribeAll(serviceIdentity)
    }

    override suspend fun isConnected(): Boolean = isConnected

    override suspend fun send(message: TransportMessage) {
        checkIsConnected()
        messageBus.publish(message)
    }

    override suspend fun subscribe(
        eventType: EventType,
        handler: MessageHandler,
    ) {
        checkIsConnected()
        messageBus.subscribe(serviceIdentity, eventType, handler)
    }

    override suspend fun unsubscribe(eventType: EventType) {
        checkIsConnected()
        messageBus.unsubscribe(serviceIdentity, eventType)
    }

    private fun checkIsConnected() {
        check(isConnected) { "Transport not connected" }
    }
}
