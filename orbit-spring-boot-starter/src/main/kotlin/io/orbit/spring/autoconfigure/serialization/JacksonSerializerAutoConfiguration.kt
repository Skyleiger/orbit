package io.orbit.spring.autoconfigure.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import io.orbit.core.serializer.EventSerializer
import io.orbit.serialization.jackson.JacksonEventSerializer
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
@ConditionalOnClass(JacksonEventSerializer::class)
class JacksonSerializerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun jacksonEventSerializer(
        @Autowired(required = false) objectMapper: ObjectMapper?,
    ): EventSerializer =
        if (objectMapper != null) {
            JacksonEventSerializer(objectMapper)
        } else {
            JacksonEventSerializer()
        }
}
