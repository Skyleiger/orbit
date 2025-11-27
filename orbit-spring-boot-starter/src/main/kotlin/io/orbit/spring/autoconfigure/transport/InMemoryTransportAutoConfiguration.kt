package io.orbit.spring.autoconfigure.transport

import io.orbit.core.transport.MessageTransport
import io.orbit.transport.inmemory.InMemoryTransport
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(
    prefix = "orbit.transport",
    name = ["type"],
    havingValue = "in-memory",
    matchIfMissing = true,
)
@ConditionalOnClass(InMemoryTransport::class)
class InMemoryTransportAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun inMemoryTransport(): MessageTransport = InMemoryTransport()
}
