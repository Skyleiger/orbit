package io.orbit.transport.inmemory

import io.orbit.core.event.EventType
import io.orbit.core.service.ServiceIdentity
import io.orbit.core.transport.MessageHandler
import io.orbit.core.transport.MessageTransport
import io.orbit.core.transport.TransportMessage

/**
 * Test constants for consistent test data.
 */
object TestConstants {
    const val TEST_MESSAGE_COUNT = 10
    const val TEST_DELAY_MS = 10L
    const val LARGE_MESSAGE_COUNT = 100
}

/**
 * Helper functions for creating test objects.
 */
object TestFactory {
    fun createMessage(
        id: String = "msg-1",
        eventType: String = "test.event",
        body: String = "test message",
    ): TransportMessage =
        TransportMessage(
            messageId = id,
            eventType = eventType,
            contentType = "application/json",
            contentEncoding = "utf-8",
            body = body.toByteArray(),
        )

    fun createEventType(name: String = "test.event"): EventType = EventType(name)

    fun createServiceIdentity(name: String = "test-service"): ServiceIdentity = ServiceIdentity(name)

    fun createHandler(onMessage: (TransportMessage) -> Unit): MessageHandler = MessageHandler { onMessage(it) }
}

/**
 * Extension functions for cleaner test code.
 */
suspend fun MessageTransport.connectAndSubscribe(
    eventType: EventType,
    handler: MessageHandler,
) {
    connect()
    subscribe(eventType, handler)
}

suspend fun MessageTransport.sendAndWait(
    message: TransportMessage,
    delayMs: Long = TestConstants.TEST_DELAY_MS,
) {
    send(message)
    kotlinx.coroutines.delay(delayMs)
}
