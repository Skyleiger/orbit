package io.orbit.spring.autoconfigure.transport

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.orbit.core.transport.TransportFactory
import io.orbit.spring.autoconfigure.OrbitProperties
import io.orbit.transport.rabbitmq.RabbitMQTransportFactory
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeansOfType
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class RabbitMQTransportAutoConfigurationTest :
    FunSpec({

        @Configuration
        @EnableConfigurationProperties(OrbitProperties::class)
        class TestConfiguration

        val contextRunner =
            ApplicationContextRunner()
                .withUserConfiguration(TestConfiguration::class.java)
                .withConfiguration(AutoConfigurations.of(RabbitMQTransportAutoConfiguration::class.java))

        test("should create RabbitMQTransportFactory when type is rabbitmq") {
            contextRunner
                .withPropertyValues("orbit.transport.type=rabbitmq")
                .run { context ->
                    val factory = context.getBean<TransportFactory>()
                    factory.shouldBeInstanceOf<RabbitMQTransportFactory>()
                }
        }

        test("should not create RabbitMQTransportFactory by default (matchIfMissing = false)") {
            contextRunner
                .run { context ->
                    context.getBeansOfType<TransportFactory>() shouldBe emptyMap()
                }
        }

        test("should not create RabbitMQTransportFactory when type is not rabbitmq") {
            contextRunner
                .withPropertyValues("orbit.transport.type=in-memory")
                .run { context ->
                    context.getBeansOfType<TransportFactory>() shouldBe emptyMap()
                }
        }
    })
