package io.orbit.spring.autoconfigure.serialization

import io.orbit.core.serializer.EventSerializer
import io.orbit.serialization.kotlinx.KotlinxEventSerializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(
    prefix = "orbit.serialization",
    name = ["type"],
    havingValue = "kotlinx",
)
@ConditionalOnClass(KotlinxEventSerializer::class)
class KotlinxSerializerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun kotlinxEventSerializer(): EventSerializer = KotlinxEventSerializer()
}
