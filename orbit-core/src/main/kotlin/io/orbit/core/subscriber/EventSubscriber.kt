package io.orbit.core.subscriber

import io.orbit.core.event.EventDefinition
import io.orbit.core.event.EventRegistry
import io.orbit.core.event.EventType
import io.orbit.core.handler.EventHandler
import io.orbit.core.handler.EventHandlerRegistry
import io.orbit.core.serializer.EventSerializer
import io.orbit.core.serializer.SerializedEvent
import io.orbit.core.transport.MessageHandler
import io.orbit.core.transport.MessageTransport

interface EventSubscriber {
    suspend fun subscribeAll()

    suspend fun unsubscribeAll()
}

internal class DefaultEventSubscriber(
    private val eventRegistry: EventRegistry,
    private val handlerRegistry: EventHandlerRegistry,
    private val serializer: EventSerializer,
    private val transport: MessageTransport,
) : EventSubscriber {
    override suspend fun subscribeAll() {
        eventRegistry.definitions.forEach { definition ->
            transport.subscribe(
                eventType = definition.eventType.value,
                handler = createMessageHandler(definition),
            )
        }
    }

    override suspend fun unsubscribeAll() {
        eventRegistry.definitions.forEach { definition ->
            transport.unsubscribe(definition.eventType.value)
        }
    }

    private fun createMessageHandler(definition: EventDefinition): MessageHandler =
        MessageHandler { message ->
            val serialized =
                SerializedEvent(
                    data = message.body,
                    contentType = message.contentType,
                    contentEncoding = message.contentEncoding,
                )

            val envelope =
                serializer.deserialize(
                    serialized,
                    definition.eventClass.java,
                )
            handleEvent(definition.eventType, envelope.event)
        }

    private suspend fun <E : Any> handleEvent(
        eventType: EventType,
        event: E,
    ) {
        val eventHandlers: List<EventHandler<E>> = handlerRegistry.getHandlers(eventType)
        eventHandlers.forEach { handler ->
            handler.handle(event)
        }
    }
}
