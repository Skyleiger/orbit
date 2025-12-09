package io.orbit.spring.autoconfigure.transport

import io.orbit.core.transport.TransportFactory
import io.orbit.spring.autoconfigure.OrbitProperties
import io.orbit.transport.rabbitmq.RabbitMQTransportConfig
import io.orbit.transport.rabbitmq.RabbitMQTransportFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(
    prefix = "orbit.transport",
    name = ["type"],
    havingValue = "rabbitmq",
)
@ConditionalOnClass(RabbitMQTransportFactory::class)
class RabbitMQTransportAutoConfiguration(
    private val properties: OrbitProperties,
) {
    @Bean
    @ConditionalOnMissingBean
    fun rabbitMQTransportConfig(): RabbitMQTransportConfig {
        val rabbitmq = properties.transport.rabbitmq
        return RabbitMQTransportConfig(
            host = rabbitmq.host,
            port = rabbitmq.port,
            username = rabbitmq.username,
            password = rabbitmq.password,
            virtualHost = rabbitmq.virtualHost,
            exchangeName = rabbitmq.exchange,
        )
    }

    @Bean
    @ConditionalOnMissingBean
    fun rabbitMQTransportFactory(config: RabbitMQTransportConfig): TransportFactory = RabbitMQTransportFactory(config)
}
