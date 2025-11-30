package io.orbit.processor.apt

import com.google.testing.compile.Compilation
import com.google.testing.compile.Compilation.Status
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.orbit.processor.OUTPUT_PATH
import java.nio.charset.StandardCharsets
import javax.tools.JavaFileObject
import javax.tools.StandardLocation

class EventAnnotationProcessorTest :
    DescribeSpec({

        describe("No events") {

            it("should not generate output file") {
                val source =
                    JavaFileObjects.forSourceString(
                        "test.NoEvent",
                        """
                        package test;
                        class NoEvent {}
                        """.trimIndent(),
                    )

                val compilation = compileWithProcessor(source)

                compilation.status() shouldBe Status.SUCCESS
                compilation.readGeneratedEventsFile() shouldBe null
            }
        }

        describe("Single event") {

            it("should generate output file with class name") {
                val source =
                    JavaFileObjects.forSourceString(
                        "test.SingleEvent",
                        """
                        package test;
                        import io.orbit.core.event.Event;

                        @Event(type = "test.single")
                        class SingleEvent {}
                        """.trimIndent(),
                    )

                val compilation = compileWithProcessor(source)

                compilation.status() shouldBe Status.SUCCESS
                compilation.readGeneratedEventsFile()?.trim() shouldBe "test.SingleEvent"
            }
        }

        describe("Multiple events") {

            it("should include only annotated classes from one file") {
                val source =
                    JavaFileObjects.forSourceString(
                        "test.MultipleEvents",
                        """
                        package test;
                        import io.orbit.core.event.Event;

                        @Event(type = "test.event.a")
                        class EventA {}

                        @Event(type = "test.event.b")
                        class EventB {}
                        
                        class NotAnEvent {}
                        """.trimIndent(),
                    )

                val compilation = compileWithProcessor(source)

                compilation.status() shouldBe Status.SUCCESS

                val content = compilation.readGeneratedEventsFile()
                content shouldContain "test.EventA"
                content shouldContain "test.EventB"
                content?.lines()?.filter { it.isNotBlank() }?.size shouldBe 2
            }

            it("should aggregate events from multiple files") {
                val file1 =
                    JavaFileObjects.forSourceString(
                        "test.File1Event",
                        """
                        package test;
                        import io.orbit.core.event.Event;

                        @Event(type = "test.file1.event.a")
                        class File1EventA {}
                        
                        @Event(type = "test.file1.event.b")
                        class File1EventB {}
                        """.trimIndent(),
                    )

                val file2 =
                    JavaFileObjects.forSourceString(
                        "test.File2Event",
                        """
                        package test;
                        import io.orbit.core.event.Event;

                        @Event(type = "test.file2.event.c")
                        class File2EventC {}
                        """.trimIndent(),
                    )

                val compilation = compileWithProcessor(file1, file2)

                compilation.status() shouldBe Status.SUCCESS

                val content = compilation.readGeneratedEventsFile()
                content shouldContain "test.File1EventA"
                content shouldContain "test.File1EventB"
                content shouldContain "test.File2EventC"
                content?.lines()?.filter { it.isNotBlank() }?.size shouldBe 3
            }
        }

        describe("Ignored type declarations") {

            it("should ignore interfaces") {
                val source =
                    JavaFileObjects.forSourceString(
                        "test.MyEventInterface",
                        """
                        package test;
                        import io.orbit.core.event.Event;

                        @Event(type = "test.interface.event")
                        interface MyEventInterface {}
                        """.trimIndent(),
                    )

                val compilation = compileWithProcessor(source)

                compilation.status() shouldBe Status.SUCCESS
                compilation.readGeneratedEventsFile() shouldBe null
            }

            it("should ignore enums") {
                val source =
                    JavaFileObjects.forSourceString(
                        "test.MyEventEnum",
                        """
                        package test;
                        import io.orbit.core.event.Event;

                        @Event(type = "test.enum.event")
                        enum MyEventEnum {
                            VALUE1, VALUE2
                        }
                        """.trimIndent(),
                    )

                val compilation = compileWithProcessor(source)

                compilation.status() shouldBe Status.SUCCESS
                compilation.readGeneratedEventsFile() shouldBe null
            }

            it("should ignore abstract classes") {
                val source =
                    JavaFileObjects.forSourceString(
                        "test.AbstractEvent",
                        """
                        package test;
                        import io.orbit.core.event.Event;

                        @Event(type = "test.abstract")
                        abstract class AbstractEvent {}
                        """.trimIndent(),
                    )

                val compilation = compileWithProcessor(source)

                compilation.status() shouldBe Status.SUCCESS
                compilation.readGeneratedEventsFile() shouldBe null
            }
        }

        describe("Supported class variations") {

            it("should process nested classes") {
                val source =
                    JavaFileObjects.forSourceString(
                        "test.OuterClass",
                        """
                        package test;
                        import io.orbit.core.event.Event;

                        class OuterClass {
                            @Event(type = "test.nested")
                            static class NestedEvent {}
                        }
                        """.trimIndent(),
                    )

                val compilation = compileWithProcessor(source)

                compilation.status() shouldBe Status.SUCCESS
                compilation.readGeneratedEventsFile()?.trim() shouldBe "test.OuterClass.NestedEvent"
            }

            it("should process generic classes") {
                val source =
                    JavaFileObjects.forSourceString(
                        "test.GenericEvent",
                        """
                        package test;
                        import io.orbit.core.event.Event;

                        @Event(type = "test.generic")
                        class GenericEvent<T> {}
                        """.trimIndent(),
                    )

                val compilation = compileWithProcessor(source)

                compilation.status() shouldBe Status.SUCCESS
                compilation.readGeneratedEventsFile()?.trim() shouldBe "test.GenericEvent"
            }
        }
    })

private fun compileWithProcessor(vararg javaSources: JavaFileObject): Compilation =
    javac()
        .withProcessors(EventAnnotationProcessor())
        .compile(*javaSources)

private fun Compilation.readGeneratedEventsFile(): String? =
    generatedFile(StandardLocation.CLASS_OUTPUT, OUTPUT_PATH)
        .map { it.openInputStream().use { stream -> stream.readBytes().toString(StandardCharsets.UTF_8) } }
        .orElse(null)
