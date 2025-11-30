package io.orbit.spring.autoconfigure.transport

import io.orbit.core.transport.MessageTransport
import io.orbit.transport.rabbitmq.RabbitMQTransport
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
@ConditionalOnClass(RabbitMQTransport::class)
class RabbitMQTransportAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun rabbitMQTransport(): MessageTransport = RabbitMQTransport()
}
