package io.orbit.core

import io.orbit.core.event.EventRegistry
import io.orbit.core.handler.EventHandlerRegistry
import io.orbit.core.publisher.EventPublisher
import io.orbit.core.serializer.EventSerializer
import io.orbit.core.service.ServiceIdentity
import io.orbit.core.subscriber.EventSubscriber
import io.orbit.core.transport.MessageTransport
import kotlinx.coroutines.runBlocking

sealed interface Orbit : AutoCloseable {
    companion object {
        const val REVISION: String = OrbitBuildConfig.REVISION
        const val VERSION: String = OrbitBuildConfig.VERSION
    }

    suspend fun connect()

    suspend fun disconnect()

    suspend fun isConnected(): Boolean

    suspend fun publish(event: Any)
}

internal class DefaultOrbit(
    private val serviceIdentity: ServiceIdentity,
    private val eventRegistry: EventRegistry,
    private val handlerRegistry: EventHandlerRegistry,
    private val serializer: EventSerializer,
    private val publisher: EventPublisher,
    private val subscriber: EventSubscriber,
    private val transport: MessageTransport,
) : Orbit {
    override suspend fun connect() {
        transport.connect()
        subscriber.subscribeAll()
    }

    override suspend fun disconnect() {
        subscriber.unsubscribeAll()
        transport.disconnect()
    }

    override suspend fun isConnected(): Boolean = transport.isConnected()

    override fun close() {
        runBlocking {
            disconnect()
        }
    }

    override suspend fun publish(event: Any) {
        publisher.publish(event)
    }
}
