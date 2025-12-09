package io.orbit.transport.rabbitmq

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for the RabbitMQ transport adapter.
 *
 * This configuration class provides all necessary settings for establishing
 * a connection to RabbitMQ and configuring the messaging behavior.
 *
 * Example usage:
 * ```kotlin
 * val config = RabbitMQTransportConfig(
 *     host = "rabbitmq.example.com",
 *     port = 5672,
 *     username = "myuser",
 *     password = "mypassword",
 *     exchangeName = "my-app.events"
 * )
 * ```
 *
 * @property host The RabbitMQ server hostname. Defaults to `localhost`.
 * @property port The RabbitMQ server AMQP port. Defaults to `5672`.
 * @property username The username for authentication. Defaults to `guest`.
 * @property password The password for authentication. Defaults to `guest`.
 * @property virtualHost The RabbitMQ virtual host to connect to. Defaults to `/`.
 * @property connectionTimeout Timeout for establishing the connection. Defaults to 30 seconds.
 * @property requestedHeartbeat Heartbeat interval for the connection. Defaults to 60 seconds.
 * @property automaticRecoveryEnabled Whether automatic connection recovery is enabled. Defaults to `true`.
 * @property networkRecoveryInterval Interval between recovery attempts. Defaults to 5 seconds.
 * @property exchangeName The name of the topic exchange for all events. Defaults to `orbit.events`.
 * @property prefetchCount The number of messages to prefetch per consumer. Defaults to `10`.
 *
 * @see RabbitMQTransport
 */
data class RabbitMQTransportConfig(
    val host: String = "localhost",
    val port: Int = 5672,
    val username: String = "guest",
    val password: String = "guest",
    val virtualHost: String = "/",
    val connectionTimeout: Duration = 30.seconds,
    val requestedHeartbeat: Duration = 60.seconds,
    val automaticRecoveryEnabled: Boolean = true,
    val networkRecoveryInterval: Duration = 5.seconds,
    val exchangeName: String = "orbit.events",
    val prefetchCount: Int = 10,
)
