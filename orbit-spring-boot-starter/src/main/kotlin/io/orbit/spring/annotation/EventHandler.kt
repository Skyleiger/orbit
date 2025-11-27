package io.orbit.spring.annotation

import org.springframework.stereotype.Component

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Component
annotation class EventHandler
