package io.orbit.spring.autoconfigure

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class OrbitPropertiesTest :
    FunSpec({

        @Configuration
        @EnableConfigurationProperties(OrbitProperties::class)
        class TestConfiguration

        val contextRunner =
            ApplicationContextRunner()
                .withUserConfiguration(TestConfiguration::class.java)

        test("should have default service name as empty string") {
            contextRunner.run { context ->
                val properties = context.getBean<OrbitProperties>()
                properties.service.name shouldBe ""
            }
        }

        test("should have default transport type as IN_MEMORY") {
            contextRunner.run { context ->
                val properties = context.getBean<OrbitProperties>()
                properties.transport.type shouldBe OrbitProperties.TransportType.IN_MEMORY
            }
        }

        test("should have default serialization type as JACKSON") {
            contextRunner.run { context ->
                val properties = context.getBean<OrbitProperties>()
                properties.serialization.type shouldBe OrbitProperties.SerializationType.JACKSON
            }
        }

        test("should have autoStartup enabled by default") {
            contextRunner.run { context ->
                val properties = context.getBean<OrbitProperties>()
                properties.autoStartup shouldBe true
            }
        }

        test("should have default RabbitMQ properties") {
            contextRunner.run { context ->
                val properties = context.getBean<OrbitProperties>()
                val rabbitmq = properties.transport.rabbitmq

                rabbitmq.host shouldBe "localhost"
                rabbitmq.port shouldBe 5672
                rabbitmq.username shouldBe "guest"
                rabbitmq.password shouldBe "guest"
                rabbitmq.virtualHost shouldBe "/"
                rabbitmq.exchange shouldBe "orbit.events"
            }
        }

        test("should allow creating properties with custom values") {
            contextRunner
                .withPropertyValues(
                    "orbit.service.name=my-service",
                    "orbit.transport.type=RABBITMQ",
                    "orbit.transport.rabbitmq.host=rabbitmq.example.com",
                    "orbit.transport.rabbitmq.port=5673",
                    "orbit.transport.rabbitmq.username=admin",
                    "orbit.transport.rabbitmq.password=secret",
                    "orbit.transport.rabbitmq.virtualHost=/production",
                    "orbit.transport.rabbitmq.exchange=my-exchange",
                    "orbit.serialization.type=KOTLINX",
                    "orbit.auto-startup=false",
                ).run { context ->
                    val properties = context.getBean<OrbitProperties>()
                    properties.service.name shouldBe "my-service"
                    properties.transport.type shouldBe OrbitProperties.TransportType.RABBITMQ
                    properties.transport.rabbitmq.host shouldBe "rabbitmq.example.com"
                    properties.transport.rabbitmq.port shouldBe 5673
                    properties.transport.rabbitmq.username shouldBe "admin"
                    properties.transport.rabbitmq.password shouldBe "secret"
                    properties.transport.rabbitmq.virtualHost shouldBe "/production"
                    properties.transport.rabbitmq.exchange shouldBe "my-exchange"
                    properties.serialization.type shouldBe OrbitProperties.SerializationType.KOTLINX
                    properties.autoStartup shouldBe false
                }
        }
    })
