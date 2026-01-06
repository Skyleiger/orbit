package io.orbit.spring.autoconfigure

import io.orbit.core.DefaultOrbitBuilder
import io.orbit.core.Orbit
import io.orbit.core.OrbitBuilder
import io.orbit.core.handler.EventHandler
import io.orbit.core.serializer.SerializerFactory
import io.orbit.core.transport.TransportFactory
import io.orbit.spring.autoconfigure.serialization.JacksonSerializerAutoConfiguration
import io.orbit.spring.autoconfigure.serialization.KotlinxSerializerAutoConfiguration
import io.orbit.spring.autoconfigure.transport.InMemoryTransportAutoConfiguration
import io.orbit.spring.autoconfigure.transport.RabbitMQTransportAutoConfiguration
import io.orbit.spring.event.SpringEventHandlerDiscovery
import io.orbit.spring.lifecycle.OrbitLifecycleManager
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import kotlin.reflect.KClass

@Configuration
@EnableConfigurationProperties(OrbitProperties::class)
@AutoConfigureAfter(
    JacksonSerializerAutoConfiguration::class,
    KotlinxSerializerAutoConfiguration::class,
    InMemoryTransportAutoConfiguration::class,
    RabbitMQTransportAutoConfiguration::class,
)
class OrbitAutoConfiguration(
    private val properties: OrbitProperties,
    private val applicationContext: ApplicationContext,
) {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @ConditionalOnMissingBean
    fun orbitBuilder(
        serializerFactory: SerializerFactory,
        transportFactory: TransportFactory,
    ): OrbitBuilder =
        DefaultOrbitBuilder()
            .service(resolveServiceName())
            .serializer(serializerFactory)
            .transport(transportFactory)
            .registerDiscoveredHandlers()

    private fun OrbitBuilder.registerDiscoveredHandlers(): OrbitBuilder {
        SpringEventHandlerDiscovery(applicationContext)
            .discoverEventHandlers()
            .forEach { (eventClass, handlers) ->
                handlers.forEach { handler ->
                    @Suppress("UNCHECKED_CAST")
                    handler(eventClass as KClass<Any>, handler as EventHandler<Any>)
                }
            }
        return this
    }

    @Bean
    @ConditionalOnMissingBean
    fun orbit(builder: OrbitBuilder): Orbit = builder.build()

    @Bean
    @ConditionalOnMissingBean
    fun orbitLifecycleManager(orbit: Orbit): OrbitLifecycleManager = OrbitLifecycleManager(orbit, properties.autoStartup)

    private fun resolveServiceName(): String {
        val serviceName = properties.service.name
        if (serviceName.isNotBlank()) {
            return serviceName
        }

        val springApplicationName = applicationContext.applicationName
        if (springApplicationName.isNotBlank()) {
            return springApplicationName
        }

        error("orbit.service.name or spring.application.name must be configured")
    }
}
