@file:OptIn(ExperimentalCompilerApi::class)

package io.orbit.processor.ksp

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.orbit.processor.OUTPUT_PATH
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.io.path.exists
import kotlin.io.path.readText

class EventSymbolProcessorTest :
    DescribeSpec({

        describe("No events") {

            it("should not generate output file") {
                val source =
                    SourceFile.kotlin(
                        "NoEvent.kt",
                        """
                        package test

                        class NoEvent
                        """.trimIndent(),
                    )

                val result = compileWithProcessor(source)

                result.exitCode shouldBe KotlinCompilation.ExitCode.OK
                result.readGeneratedEventsFile() shouldBe null
            }
        }

        describe("Single event") {

            it("should generate output file with class name") {
                val source =
                    SourceFile.kotlin(
                        "SingleEvent.kt",
                        """
                        package test
                        import io.orbit.core.event.Event

                        @Event(type = "test.single")
                        class SingleEvent
                        """.trimIndent(),
                    )

                val result = compileWithProcessor(source)

                result.exitCode shouldBe KotlinCompilation.ExitCode.OK
                result.readGeneratedEventsFile()?.trim() shouldBe "test.SingleEvent"
            }
        }

        describe("Multiple events") {

            it("should include only annotated classes from one file") {
                val source =
                    SourceFile.kotlin(
                        "MultipleEvents.kt",
                        """
                        package test
                        import io.orbit.core.event.Event

                        @Event(type = "test.event.a")
                        class EventA

                        @Event(type = "test.event.b")
                        class EventB

                        class NotAnEvent
                        """.trimIndent(),
                    )

                val result = compileWithProcessor(source)

                result.exitCode shouldBe KotlinCompilation.ExitCode.OK

                val content = result.readGeneratedEventsFile()
                content shouldContain "test.EventA"
                content shouldContain "test.EventB"
                content?.lines()?.filter { it.isNotBlank() }?.size shouldBe 2
            }

            it("should aggregate events from multiple files") {
                val file1 =
                    SourceFile.kotlin(
                        "File1Events.kt",
                        """
                        package test
                        import io.orbit.core.event.Event

                        @Event(type = "test.file1.event.a")
                        class File1EventA

                        @Event(type = "test.file1.event.b")
                        class File1EventB
                        """.trimIndent(),
                    )

                val file2 =
                    SourceFile.kotlin(
                        "File2Events.kt",
                        """
                        package test
                        import io.orbit.core.event.Event

                        @Event(type = "test.file2.event.c")
                        class File2EventC
                        """.trimIndent(),
                    )

                val result = compileWithProcessor(file1, file2)

                result.exitCode shouldBe KotlinCompilation.ExitCode.OK

                val content = result.readGeneratedEventsFile()
                content shouldContain "test.File1EventA"
                content shouldContain "test.File1EventB"
                content shouldContain "test.File2EventC"
                content?.lines()?.filter { it.isNotBlank() }?.size shouldBe 3
            }
        }

        describe("Ignored type declarations") {

            it("should ignore interfaces") {
                val source =
                    SourceFile.kotlin(
                        "MyEventInterface.kt",
                        """
                        package test
                        import io.orbit.core.event.Event

                        @Event(type = "test.interface.event")
                        interface MyEventInterface
                        """.trimIndent(),
                    )

                val result = compileWithProcessor(source)

                result.exitCode shouldBe KotlinCompilation.ExitCode.OK
                result.readGeneratedEventsFile() shouldBe null
            }

            it("should ignore enums") {
                val source =
                    SourceFile.kotlin(
                        "MyEventEnum.kt",
                        """
                        package test
                        import io.orbit.core.event.Event

                        @Event(type = "test.enum.event")
                        enum class MyEventEnum {
                            VALUE1, VALUE2
                        }
                        """.trimIndent(),
                    )

                val result = compileWithProcessor(source)

                result.exitCode shouldBe KotlinCompilation.ExitCode.OK
                result.readGeneratedEventsFile() shouldBe null
            }

            it("should ignore abstract classes") {
                val source =
                    SourceFile.kotlin(
                        "AbstractEvent.kt",
                        """
                        package test
                        import io.orbit.core.event.Event

                        @Event(type = "test.abstract")
                        abstract class AbstractEvent
                        """.trimIndent(),
                    )

                val result = compileWithProcessor(source)

                result.exitCode shouldBe KotlinCompilation.ExitCode.OK
                result.readGeneratedEventsFile() shouldBe null
            }
        }

        describe("Supported class variations") {

            it("should process nested classes") {
                val source =
                    SourceFile.kotlin(
                        "OuterClass.kt",
                        """
                        package test
                        import io.orbit.core.event.Event

                        class OuterClass {
                            @Event(type = "test.nested")
                            class NestedEvent
                        }
                        """.trimIndent(),
                    )

                val result = compileWithProcessor(source)

                result.exitCode shouldBe KotlinCompilation.ExitCode.OK
                result.readGeneratedEventsFile()?.trim() shouldBe "test.OuterClass.NestedEvent"
            }

            it("should process generic classes") {
                val source =
                    SourceFile.kotlin(
                        "GenericEvent.kt",
                        """
                        package test
                        import io.orbit.core.event.Event

                        @Event(type = "test.generic")
                        class GenericEvent<T>
                        """.trimIndent(),
                    )

                val result = compileWithProcessor(source)

                result.exitCode shouldBe KotlinCompilation.ExitCode.OK
                result.readGeneratedEventsFile()?.trim() shouldBe "test.GenericEvent"
            }
        }
    })

private fun compileWithProcessor(vararg sourceFiles: SourceFile): JvmCompilationResult =
    KotlinCompilation()
        .apply {
            sources = sourceFiles.toList()
            configureKsp {
                symbolProcessorProviders += mutableListOf(EventSymbolProcessorProvider())
            }
            inheritClassPath = true
        }.compile()

private fun JvmCompilationResult.readGeneratedEventsFile(): String? {
    val path = outputDirectory.toPath().resolveSibling("ksp/sources/resources/$OUTPUT_PATH")
    return if (path.exists()) path.readText() else null
}
