package io.orbit.transport.rabbitmq.internal

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.impl.ClientVersion
import com.rabbitmq.client.impl.LongStringHelper
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
 * | `orbit_name` | Orbit service name |
 * | `orbit_version` | Orbit service version |
 * | `rabbitmq_client_version` | RabbitMQ client version |
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
                factory.newConnection("orbit:${serviceIdentity.source}")
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

            clientProperties = buildClientProperties(clientProperties)
        }

    private fun buildClientProperties(defaultProperties: Map<String, Any>): Map<String, Any> =
        mapOf(
            "orbit_revision" to Orbit.REVISION,
            "orbit_version" to Orbit.VERSION,
            "rabbitmq_client_version" to LongStringHelper.asLongString(ClientVersion.VERSION),
            "capabilities" to (defaultProperties["capabilities"] ?: emptyMap<String, Any>()),
        )
}
