package io.orbit.core

import io.orbit.core.event.DefaultEventRegistry
import io.orbit.core.event.EventDefinition
import io.orbit.core.event.EventIntrospector
import io.orbit.core.event.EventRegistry
import io.orbit.core.event.EventType
import io.orbit.core.event.ReflectionEventIntrospector
import io.orbit.core.handler.DefaultEventHandlerRegistry
import io.orbit.core.handler.EventHandler
import io.orbit.core.handler.EventHandlerRegistry
import io.orbit.core.publisher.DefaultEventPublisher
import io.orbit.core.publisher.EventPublisher
import io.orbit.core.serializer.EventSerializer
import io.orbit.core.transport.MessageTransport
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass

interface Orbit : AutoCloseable {
    val isConnected: Boolean

    suspend fun connect()

    suspend fun disconnect()

    suspend fun publish(event: Any)
}

internal class DefaultOrbit(
    private val eventRegistry: EventRegistry,
    private val handlerRegistry: EventHandlerRegistry,
    private val publisher: EventPublisher,
    private val serializer: EventSerializer,
    private val transport: MessageTransport,
) : Orbit {
    override val isConnected: Boolean
        get() = TODO("Not yet implemented")

    override suspend fun connect() {
        TODO("Not yet implemented")
    }

    override suspend fun disconnect() {
        TODO("Not yet implemented")
    }

    override fun close() {
        runBlocking {
            disconnect()
        }
    }

    override suspend fun publish(event: Any) {
        publisher.publish(event)
    }
}

interface OrbitBuilder {
    fun serializer(serializer: EventSerializer): OrbitBuilder

    fun transport(transport: MessageTransport): OrbitBuilder

    fun event(eventClass: KClass<*>): OrbitBuilder

    fun <E : Any> handler(
        eventClass: KClass<E>,
        handler: EventHandler<E>,
    ): OrbitBuilder

    fun build(): Orbit
}

class DefaultOrbitBuilder : OrbitBuilder {
    private var serializer: EventSerializer? = null
    private var transport: MessageTransport? = null
    private val events: MutableMap<KClass<*>, MutableList<EventHandler<*>>> = mutableMapOf()

    override fun serializer(serializer: EventSerializer): OrbitBuilder {
        this.serializer = serializer
        return this
    }

    override fun transport(transport: MessageTransport): OrbitBuilder {
        this.transport = transport
        return this
    }

    override fun event(eventClass: KClass<*>): OrbitBuilder {
        events.putIfAbsent(eventClass, mutableListOf())
        return this
    }

    override fun <E : Any> handler(
        eventClass: KClass<E>,
        handler: EventHandler<E>,
    ): OrbitBuilder {
        events.computeIfAbsent(eventClass) { mutableListOf() }.add(handler)
        return this
    }

    override fun build(): Orbit {
        val eventDefinitions = introspectEvents()
        val handlers = collectHandlers(eventDefinitions)

        val eventRegistry = DefaultEventRegistry(eventDefinitions)
        val handlerRegistry = DefaultEventHandlerRegistry(handlers)
        val eventPublisher = DefaultEventPublisher()

        return DefaultOrbit(
            eventRegistry = eventRegistry,
            handlerRegistry = handlerRegistry,
            publisher = eventPublisher,
            serializer = checkNotNull(serializer) { "Serializer must be provided" },
            transport = checkNotNull(transport) { "Transport must be provided" },
        )
    }

    private fun introspectEvents(): List<EventDefinition> {
        val introspector: EventIntrospector = ReflectionEventIntrospector()

        return events.keys.map { eventClass ->
            introspector.introspect(eventClass)
        }
    }

    private fun collectHandlers(eventDefinitions: List<EventDefinition>): Map<EventType, List<EventHandler<*>>> {
        val eventsByClass = eventDefinitions.associateBy { it.eventClass }

        return events.mapKeys { (eventClass, _) ->
            eventsByClass[eventClass]?.eventType!!
        }
    }
}

fun orbit(block: OrbitBuilder.() -> Unit): Orbit = DefaultOrbitBuilder().apply(block).build()
