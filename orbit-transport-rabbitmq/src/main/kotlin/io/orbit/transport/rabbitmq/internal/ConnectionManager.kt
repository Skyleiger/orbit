package io.orbit.transport.rabbitmq.internal

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import io.orbit.core.Orbit
import io.orbit.core.service.ServiceIdentity
import io.orbit.transport.rabbitmq.RabbitMQTransportConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages the RabbitMQ connection lifecycle.
 *
 * This class is responsible for:
 * - Creating and configuring the [ConnectionFactory]
 * - Establishing connections with Orbit metadata in client properties
 * - Managing the connection and channel lifecycle
 *
 * ## Client Properties
 *
 * The connection is configured with the following client properties visible
 * in RabbitMQ management UI:
 *
 * | Property | Description |
 * |----------|-------------|
 * | `connection_name` | `orbit:{serviceName}-{serviceId}` |
 * | `product` | `orbit` |
 * | `version` | Orbit version |
 * | `platform` | Kotlin version |
 * | `service_name` | The service name from [ServiceIdentity] |
 * | `service_id` | The unique service instance ID |
 * | `capabilities` | RabbitMQ client capabilities |
 *
 * @param config The RabbitMQ transport configuration.
 * @param serviceIdentity The service identity for metadata.
 *
 * @see RabbitMQTransportConfig
 */
internal class ConnectionManager(
    private val config: RabbitMQTransportConfig,
    private val serviceIdentity: ServiceIdentity,
) {
    private var connection: Connection? = null

    /**
     * The active RabbitMQ channel, or `null` if not connected.
     */
    var channel: Channel? = null
        private set

    /**
     * Establishes a connection to RabbitMQ and creates a channel.
     *
     * @return The created [Channel] for messaging operations.
     * @throws java.io.IOException if the connection cannot be established.
     */
    suspend fun connect(): Channel {
        val factory = createConnectionFactory()

        connection =
            withContext(Dispatchers.IO) {
                factory.newConnection(connectionName)
            }

        channel =
            withContext(Dispatchers.IO) {
                connection!!.createChannel()
            }

        return channel!!
    }

    /**
     * Closes the connection and channel gracefully.
     *
     * This method catches and ignores any exceptions during close operations
     * to ensure cleanup completes even if the connection is already closed.
     */
    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            runCatching { channel?.close() }
            runCatching { connection?.close() }
        }
        channel = null
        connection = null
    }

    /**
     * Checks if the connection and channel are both open.
     *
     * @return `true` if connected and ready, `false` otherwise.
     */
    suspend fun isConnected(): Boolean =
        withContext(Dispatchers.IO) {
            connection?.isOpen == true && channel?.isOpen == true
        }

    private fun createConnectionFactory() =
        ConnectionFactory().apply {
            host = config.host
            port = config.port
            username = config.username
            password = config.password
            virtualHost = config.virtualHost
            connectionTimeout = config.connectionTimeout.inWholeMilliseconds.toInt()
            requestedHeartbeat = config.requestedHeartbeat.inWholeSeconds.toInt()
            isAutomaticRecoveryEnabled = config.automaticRecoveryEnabled
            networkRecoveryInterval = config.networkRecoveryInterval.inWholeMilliseconds

            clientProperties = buildClientProperties()
        }

    private fun buildClientProperties(): Map<String, Any> =
        mapOf(
            "connection_name" to connectionName,
            "product" to "orbit",
            "version" to Orbit.VERSION,
            "platform" to "Kotlin ${KotlinVersion.CURRENT}",
            "service_name" to serviceIdentity.name.value,
            "service_id" to serviceIdentity.id.value,
            "capabilities" to
                mapOf(
                    "publisher_confirms" to true,
                    "consumer_cancel_notify" to true,
                    "exchange_exchange_bindings" to true,
                    "basic.nack" to true,
                    "connection.blocked" to true,
                ),
        )

    private val connectionName: String
        get() = "orbit:${serviceIdentity.source}"
}
