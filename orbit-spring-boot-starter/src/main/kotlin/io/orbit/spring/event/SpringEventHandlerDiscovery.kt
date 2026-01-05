package io.orbit.spring.event

import io.orbit.core.handler.EventHandler
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.core.MethodIntrospector
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.util.ClassUtils
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.jvm.kotlinFunction
import io.orbit.spring.annotation.EventHandler as EventHandlerAnnotation

/**
 * Discovers and adapts Spring beans with @[EventHandler] annotated methods
 * into Orbit [EventHandler] instances.
 *
 * This class scans the Spring [ApplicationContext] for beans containing methods
 * annotated with [io.orbit.spring.annotation.EventHandler] and creates appropriate
 * adapters based on whether the method is a Kotlin suspend function or regular Java method.
 *
 * ## Discovery Process
 * 1. Scan all bean definitions in the application context
 * 2. Find methods annotated with @[EventHandler]
 * 3. Create [JavaEventHandlerAdapter] or [KotlinEventHandlerAdapter] based on method type
 * 4. Group handlers by event type
 *
 * ## Supported Handler Types
 * - Regular Java methods: `fun handleEvent(event: MyEvent)`
 * - Kotlin suspend functions: `suspend fun handleEvent(event: MyEvent)`
 *
 * @param applicationContext The Spring application context to scan for event handlers
 */
class SpringEventHandlerDiscovery(
    private val applicationContext: ApplicationContext,
) {
    /**
     * Discovers all event handlers in the Spring application context.
     *
     * @return A map of event classes with their corresponding event handlers
     */
    fun discoverEventHandlers(): Map<KClass<*>, List<EventHandler<*>>> =
        applicationContext.beanDefinitionNames
            .mapNotNull { beanName -> discoverBean(beanName) }
            .flatMap { (bean, methods) ->
                methods.map { method -> createEventHandlerPair(bean, method) }
            }.groupBy({ it.first }, { it.second })

    private fun discoverBean(beanName: String): Pair<Any, Set<Method>>? {
        val type = applicationContext.getType(beanName) ?: return null
        val methods = findEventHandlerMethods(type)

        if (methods.isEmpty()) return null

        val bean = getBean(beanName) ?: return null

        return bean to methods
    }

    private fun createEventHandlerPair(
        bean: Any,
        method: Method,
    ): Pair<KClass<Any>, EventHandler<Any>> {
        val eventClass = getEventClassFromMethod(method)
        val adapter = createAdapter(bean, method)
        return eventClass to adapter
    }

    private fun findEventHandlerMethods(beanType: Class<*>): Set<Method> {
        // Use ClassUtils.getUserClass to unwrap CGLIB proxies and get the actual class
        val targetClass = ClassUtils.getUserClass(beanType)
        return MethodIntrospector
            .selectMethods(targetClass) {
                AnnotatedElementUtils.findMergedAnnotation(it, EventHandlerAnnotation::class.java) != null
            }
    }

    private fun getBean(beanName: String): Any? =
        try {
            applicationContext.getBean(beanName)
        } catch (_: BeansException) {
            null
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
