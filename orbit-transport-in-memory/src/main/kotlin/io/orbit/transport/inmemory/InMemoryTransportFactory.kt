package io.orbit.transport.inmemory

import io.orbit.core.service.ServiceIdentity
import io.orbit.core.transport.MessageTransport
import io.orbit.core.transport.TransportFactory

/**
 * Factory for creating [InMemoryTransport] instances that share a common message bus.
 *
 * This factory creates separate transport instances for each Orbit instance,
 * allowing them to maintain independent connection states and handler registrations.
 * However, all instances created by the same factory share a common [InMemoryMessageBus],
 * enabling message delivery between different Orbit instances.
 *
 * ## Usage Example
 *
 * ```kotlin
 * // Create a shared factory
 * val factory = InMemoryTransportFactory()
 *
 * // Each orbit instance gets its own transport
 * val publisher = orbit {
 *     service("publisher")
 *     transport(factory)  // Own transport instance
 * }
 *
 * val subscriber = orbit {
 *     service("subscriber")
 *     transport(factory)  // Own transport instance, shared bus
 * }
 *
 * // They can communicate through the shared message bus
 * publisher.connect()
 * subscriber.connect()
 * publisher.publish(MyEvent())  // Received by subscriber
 * ```
 */
class InMemoryTransportFactory : TransportFactory {
    private val sharedMessageBus = InMemoryMessageBus()

    override fun create(serviceIdentity: ServiceIdentity): MessageTransport = InMemoryTransport(sharedMessageBus, serviceIdentity)
}
