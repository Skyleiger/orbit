package io.orbit.spring.event

import io.orbit.core.handler.EventHandler
import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend

internal class JavaEventHandlerAdapter<E>(
    private val bean: Any,
    private val method: Method,
) : EventHandler<E> {
    override suspend fun handle(event: E) {
        method.invoke(bean, event)
    }
}

internal class KotlinEventHandlerAdapter<E>(
    private val bean: Any,
    private val function: KFunction<*>,
) : EventHandler<E> {
    override suspend fun handle(event: E) {
        function.callSuspend(bean, event)
    }
}
