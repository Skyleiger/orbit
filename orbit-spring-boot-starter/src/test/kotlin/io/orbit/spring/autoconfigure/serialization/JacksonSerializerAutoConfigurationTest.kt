package io.orbit.spring.autoconfigure.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.orbit.core.serializer.SerializerFactory
import io.orbit.serialization.jackson.JacksonSerializerFactory
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeansOfType
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class JacksonSerializerAutoConfigurationTest :
    FunSpec({

        val contextRunner =
            ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonSerializerAutoConfiguration::class.java))

        test("should create JacksonSerializerFactory when type is jackson") {
            contextRunner
                .withPropertyValues("orbit.serialization.type=jackson")
                .run { context ->
                    val factory = context.getBean<SerializerFactory>()
                    factory.shouldBeInstanceOf<JacksonSerializerFactory>()
                }
        }

        test("should create JacksonSerializerFactory by default (matchIfMissing = true)") {
            contextRunner
                .run { context ->
                    val factory = context.getBean<SerializerFactory>()
                    factory.shouldBeInstanceOf<JacksonSerializerFactory>()
                }
        }

        test("should not create JacksonSerializerFactory when type is not jackson") {
            contextRunner
                .withPropertyValues("orbit.serialization.type=abc")
                .run { context ->
                    context.getBeansOfType<SerializerFactory>() shouldBe emptyMap()
                }
        }

        test("should use provided ObjectMapper when available") {
            contextRunner
                .withPropertyValues("orbit.serialization.type=jackson")
                .withBean(ObjectMapper::class.java, {
                    ObjectMapper()
                })
                .run { context ->
                    val factory = context.getBean<SerializerFactory>()
                    factory.shouldBeInstanceOf<JacksonSerializerFactory>()
                }
        }
    })
