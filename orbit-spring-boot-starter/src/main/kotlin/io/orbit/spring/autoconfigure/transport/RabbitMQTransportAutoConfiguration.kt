package io.orbit.spring.autoconfigure.transport

import io.orbit.core.transport.TransportFactory
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
class RabbitMQTransportAutoConfiguration{
    @Bean
    @ConditionalOnMissingBean
    fun rabbitMQTransportFactory(): TransportFactory = RabbitMQTransportFactory()
}
