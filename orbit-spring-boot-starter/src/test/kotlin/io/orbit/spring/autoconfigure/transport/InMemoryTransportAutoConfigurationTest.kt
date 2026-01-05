package io.orbit.spring.autoconfigure.transport

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.orbit.core.transport.TransportFactory
import io.orbit.transport.inmemory.InMemoryTransportFactory
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeansOfType
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class InMemoryTransportAutoConfigurationTest :
    FunSpec({

        val contextRunner =
            ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(InMemoryTransportAutoConfiguration::class.java))

        test("should create InMemoryTransportFactory when type is in-memory") {
            contextRunner
                .withPropertyValues("orbit.transport.type=in-memory")
                .run { context ->
                    val factory = context.getBean<TransportFactory>()
                    factory.shouldBeInstanceOf<InMemoryTransportFactory>()
                }
        }

        test("should create InMemoryTransportFactory by default (matchIfMissing = true)") {
            contextRunner
                .run { context ->
                    val factory = context.getBean<TransportFactory>()
                    factory.shouldBeInstanceOf<InMemoryTransportFactory>()
                }
        }

        test("should not create InMemoryTransportFactory when type is not in-memory") {
            contextRunner
                .withPropertyValues("orbit.transport.type=abc")
                .run { context ->
                    context.getBeansOfType<TransportFactory>() shouldBe emptyMap()
                }
        }
    })
