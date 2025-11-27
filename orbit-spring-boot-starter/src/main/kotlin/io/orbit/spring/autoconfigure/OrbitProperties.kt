package io.orbit.spring.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "orbit")
data class OrbitProperties(
    val service: ServiceProperties = ServiceProperties(),
    val transport: TransportProperties = TransportProperties(),
    val serialization: SerializationProperties = SerializationProperties(),
    val autoStartup: Boolean = true,
) {
    data class ServiceProperties(
        val name: String = "", // Required, will be validated
    )

    data class TransportProperties(
        val type: TransportType = TransportType.IN_MEMORY,
        val rabbitmq: RabbitMQProperties = RabbitMQProperties(),
    )

    data class SerializationProperties(
        val type: SerializationType = SerializationType.JACKSON,
    )

    data class RabbitMQProperties(
        val host: String = "localhost",
        val port: Int = 5672,
        val username: String = "guest",
        val password: String = "guest",
        val virtualHost: String = "/",
        val exchange: String = "orbit.events",
    )

    enum class TransportType {
        IN_MEMORY,
        RABBITMQ,
    }

    enum class SerializationType {
        JACKSON,
        KOTLINX,
    }
}
