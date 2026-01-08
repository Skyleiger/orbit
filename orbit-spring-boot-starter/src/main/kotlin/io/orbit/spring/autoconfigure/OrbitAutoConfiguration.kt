package io.orbit.spring.autoconfigure

import io.orbit.core.Orbit
import io.orbit.core.OrbitBuilder
import io.orbit.core.handler.EventHandler
import io.orbit.core.orbit
import io.orbit.core.serializer.SerializerFactory
import io.orbit.core.transport.TransportFactory
import io.orbit.spring.autoconfigure.serialization.JacksonSerializerAutoConfiguration
import io.orbit.spring.autoconfigure.serialization.KotlinxSerializerAutoConfiguration
import io.orbit.spring.autoconfigure.transport.InMemoryTransportAutoConfiguration
import io.orbit.spring.autoconfigure.transport.RabbitMQTransportAutoConfiguration
import io.orbit.spring.event.SpringEventHandlerDiscovery
import io.orbit.spring.lifecycle.OrbitLifecycleManager
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
    @ConditionalOnMissingBean
    fun orbit(
        serializerFactory: SerializerFactory,
        transportFactory: TransportFactory,
        customizers: ObjectProvider<OrbitCustomizer>,
    ): Orbit =
        orbit {
            service(resolveServiceName())
            serializer(serializerFactory)
            transport(transportFactory)

            discoverHandlers(this)
            applyCustomizers(this, customizers)
        }

    private fun discoverHandlers(builder: OrbitBuilder) {
        SpringEventHandlerDiscovery(applicationContext)
            .discoverEventHandlers()
            .forEach { (eventClass, handlers) ->
                handlers.forEach { handler ->
                    @Suppress("UNCHECKED_CAST")
                    builder.handler(eventClass as KClass<Any>, handler as EventHandler<Any>)
                }
            }
    }

    private fun applyCustomizers(
        builder: OrbitBuilder,
        customizers: ObjectProvider<OrbitCustomizer>,
    ) {
        customizers.orderedStream().forEach { it.customize(builder) }
    }

    @Bean
    fun orbitLifecycleManager(orbit: Orbit): OrbitLifecycleManager = OrbitLifecycleManager(orbit, properties.autoStartup)

    private fun resolveServiceName(): String {
        val serviceName = properties.service.name
        if (serviceName.isNotBlank()) {
            return serviceName
        }

        // Prefer the Spring Boot standard property. This is reliably available via the Environment,
        // including in tests using ApplicationContextRunner.
        val appName = applicationContext.environment.getProperty("spring.application.name")
        if (!appName.isNullOrBlank()) {
            return appName
        }

        // Fallback for non-Boot or custom ApplicationContext setups where the context "applicationName"
        // is populated by the context implementation rather than Spring Boot configuration.
        val springApplicationName = applicationContext.applicationName
        if (springApplicationName.isNotBlank()) {
            return springApplicationName
        }

        error("orbit.service.name or spring.application.name must be configured")
    }
}
