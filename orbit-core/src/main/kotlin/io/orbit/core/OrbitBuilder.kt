package io.orbit.core

import io.orbit.core.event.DefaultEventRegistry
import io.orbit.core.event.EventDefinition
import io.orbit.core.event.EventIntrospector
import io.orbit.core.event.EventType
import io.orbit.core.event.ReflectionEventIntrospector
import io.orbit.core.handler.DefaultEventHandlerRegistry
import io.orbit.core.handler.EventHandler
import io.orbit.core.publisher.DefaultEventPublisher
import io.orbit.core.serializer.SerializerFactory
import io.orbit.core.service.ServiceIdentity
import io.orbit.core.subscriber.DefaultEventSubscriber
import io.orbit.core.transport.TransportFactory
import kotlin.reflect.KClass

sealed interface OrbitBuilder {
    fun service(serviceName: String): OrbitBuilder

    fun serializer(factory: SerializerFactory): OrbitBuilder

    fun transport(factory: TransportFactory): OrbitBuilder

    fun event(eventClass: KClass<*>): OrbitBuilder

    fun <E : Any> handler(
        eventClass: KClass<E>,
        handler: EventHandler<E>,
    ): OrbitBuilder

    fun build(): Orbit
}

private class DefaultOrbitBuilder : OrbitBuilder {
    private var serializerFactory: SerializerFactory? = null
    private var transportFactory: TransportFactory? = null
    private var serviceName: String? = null
    private val events: MutableMap<KClass<*>, MutableList<EventHandler<*>>> = mutableMapOf()

    override fun service(serviceName: String): OrbitBuilder {
        this.serviceName = serviceName
        return this
    }

    override fun serializer(factory: SerializerFactory): OrbitBuilder {
        this.serializerFactory = factory
        return this
    }

    override fun transport(factory: TransportFactory): OrbitBuilder {
        this.transportFactory = factory
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

        val effectiveServiceName =
            checkNotNull(serviceName) {
                "Service name must be provided"
            }

        val service = ServiceIdentity(effectiveServiceName)

        val effectiveSerializer =
            checkNotNull(serializerFactory) {
                "Serializer must be provided"
            }.create(service)

        val effectiveTransport =
            checkNotNull(transportFactory) {
                "Transport must be provided"
            }.create(service)

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
            val definition = eventsByClass[eventClass]
            checkNotNull(definition) {
                "No EventDefinition found for event class ${eventClass.qualifiedName ?: eventClass} " +
                    "(this is an internal error; the builder should have introspected all registered events)."
            }

            return@mapKeys definition.eventType
        }
    }
}

fun orbit(block: OrbitBuilder.() -> Unit): Orbit = DefaultOrbitBuilder().apply(block).build()
