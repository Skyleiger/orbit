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
import io.orbit.spring.event.JavaEventHandlerAdapter
import io.orbit.spring.event.KotlinEventHandlerAdapter
import io.orbit.spring.lifecycle.OrbitLifecycleManager
import org.springframework.aop.support.AopUtils
import org.springframework.beans.BeansException
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodIntrospector
import org.springframework.core.annotation.AnnotatedElementUtils
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.jvm.kotlinFunction
import io.orbit.spring.annotation.EventHandler as EventHandlerAnnotation

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
    fun orbitBuilder(
        serializerFactory: SerializerFactory,
        transportFactory: TransportFactory,
    ): OrbitBuilder =
        DefaultOrbitBuilder()
            .service(resolveServiceName())
            .serializer(serializerFactory)
            .transport(transportFactory)
            .apply {
                discoverEventHandlers().forEach { (eventClass, handlers) ->
                    handlers.forEach { handler ->
                        @Suppress("UNCHECKED_CAST")
                        handler(eventClass as KClass<Any>, handler as EventHandler<Any>)
                    }
                }
            }

    @Bean
    @ConditionalOnMissingBean
    fun orbit(builder: OrbitBuilder): Orbit = builder.build()

    @Bean
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

    private fun discoverEventHandlers(): Map<KClass<*>, List<EventHandler<*>>> =
        applicationContext.beanDefinitionNames
            .mapNotNull { getBean(it) }
            .flatMap { bean ->
                findEventHandlerMethods(bean).map { method ->
                    getEventClassFromMethod(method) to createAdapter(bean, method)
                }
            }.groupBy({ it.first }, { it.second })

    private fun getBean(beanName: String): Any? =
        try {
            applicationContext.getBean(beanName)
        } catch (_: BeansException) {
            null
        }

    private fun findEventHandlerMethods(bean: Any): Set<Method> {
        val targetClass = AopUtils.getTargetClass(bean)
        return MethodIntrospector
            .selectMethods(targetClass) {
                AnnotatedElementUtils.findMergedAnnotation(it, EventHandlerAnnotation::class.java) != null
            }.toSet()
    }

    private fun getEventClassFromMethod(method: Method): KClass<Any> {
        val eventParameter =
            method.parameters.getOrNull(0)
                ?: error("@EventHandler method ${method.name} must have an event parameter")

        @Suppress("UNCHECKED_CAST")
        return eventParameter.type.kotlin as? KClass<Any>
            ?: error("@EventHandler method ${method.name} event parameter must be a class")
    }

    private fun createAdapter(
        bean: Any,
        method: Method,
    ): EventHandler<Any> {
        val kotlinFunction = method.kotlinFunction
        return if (kotlinFunction != null) {
            KotlinEventHandlerAdapter(bean, kotlinFunction)
        } else {
            JavaEventHandlerAdapter(bean, method)
        }
    }
}
