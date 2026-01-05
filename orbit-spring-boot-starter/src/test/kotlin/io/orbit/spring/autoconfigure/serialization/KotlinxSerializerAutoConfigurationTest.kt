package io.orbit.spring.autoconfigure.serialization

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.orbit.core.serializer.SerializerFactory
import io.orbit.serialization.kotlinx.KotlinxSerializerFactory
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeansOfType
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class KotlinxSerializerAutoConfigurationTest :
    FunSpec({

        val contextRunner =
            ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(KotlinxSerializerAutoConfiguration::class.java))

        test("should create KotlinxSerializerFactory when type is kotlinx") {
            contextRunner
                .withPropertyValues("orbit.serialization.type=kotlinx")
                .run { context ->
                    val factory = context.getBean<SerializerFactory>()
                    factory.shouldBeInstanceOf<KotlinxSerializerFactory>()
                }
        }

        test("should not create KotlinxSerializerFactory by default (matchIfMissing = false)") {
            contextRunner
                .run { context ->
                    context.getBeansOfType<SerializerFactory>() shouldBe emptyMap()
                }
        }

        test("should not create KotlinxSerializerFactory when type is not kotlinx") {
            contextRunner
                .withPropertyValues("orbit.serialization.type=abc")
                .run { context ->
                    context.getBeansOfType<SerializerFactory>() shouldBe emptyMap()
                }
        }

        test("should use provided Json when available") {
            contextRunner
                .withPropertyValues("orbit.serialization.type=kotlinx")
                .withBean(Json::class.java, {
                    Json { }
                })
                .run { context ->
                    val factory = context.getBean<SerializerFactory>()
                    factory.shouldBeInstanceOf<KotlinxSerializerFactory>()
                }
        }
    })
