package io.orbit.spring.autoconfigure.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import io.orbit.core.serializer.SerializerFactory
import io.orbit.serialization.jackson.JacksonSerializerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(
    prefix = "orbit.serialization",
    name = ["type"],
    havingValue = "jackson",
    matchIfMissing = true,
)
@ConditionalOnClass(JacksonSerializerFactory::class)
class JacksonSerializerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun jacksonSerializerFactory(
        @Autowired(required = false) objectMapper: ObjectMapper?,
    ): SerializerFactory = JacksonSerializerFactory(objectMapper)
}
