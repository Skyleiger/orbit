package io.orbit.core

import io.orbit.core.event.DefaultEventRegistry
import io.orbit.core.event.EventDefinition
import io.orbit.core.event.EventIntrospector
import io.orbit.core.event.EventType
import io.orbit.core.event.ReflectionEventIntrospector
import io.orbit.core.handler.DefaultEventHandlerRegistry
import io.orbit.core.handler.EventHandler
import io.orbit.core.publisher.DefaultEventPublisher
import io.orbit.core.serializer.EventSerializer
import io.orbit.core.service.ServiceIdentity
import io.orbit.core.subscriber.DefaultEventSubscriber
import io.orbit.core.transport.MessageTransport
import kotlin.reflect.KClass

interface OrbitBuilder {
    fun service(serviceName: String): OrbitBuilder

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
    private var serviceName: String? = null
    private val events: MutableMap<KClass<*>, MutableList<EventHandler<*>>> = mutableMapOf()

    override fun service(serviceName: String): OrbitBuilder {
        this.serviceName = serviceName
        return this
    }

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

        val effectiveSerializer =
            checkNotNull(serializer) {
                "Serializer must be provided"
            }
        val effectiveTransport =
            checkNotNull(transport) {
                "Transport must be provided"
            }
        val effectiveServiceName =
            checkNotNull(serviceName) {
                "Service name must be provided"
            }

        val service = ServiceIdentity(effectiveServiceName)

        val eventPublisher =
            DefaultEventPublisher(
                eventRegistry = eventRegistry,
                serializer = effectiveSerializer,
                transport = effectiveTransport,
                service = service,
            )

        val eventSubscriber =
            DefaultEventSubscriber(
                eventRegistry = eventRegistry,
                handlerRegistry = handlerRegistry,
                serializer = effectiveSerializer,
                transport = effectiveTransport,
            )

        return DefaultOrbit(
            serviceIdentity = service,
            eventRegistry = eventRegistry,
            handlerRegistry = handlerRegistry,
            serializer = effectiveSerializer,
            publisher = eventPublisher,
            subscriber = eventSubscriber,
            transport = effectiveTransport,
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
