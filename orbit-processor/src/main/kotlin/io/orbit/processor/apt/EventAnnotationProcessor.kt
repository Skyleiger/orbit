package io.orbit.processor.apt

import io.orbit.core.event.Event
import io.orbit.processor.OUTPUT_PATH
import io.orbit.processor.writeEventsFile
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.StandardLocation

/**
 * Java Annotation Processor (APT) implementation of the Orbit Event Processor.
 *
 * This processor scans for all classes annotated with [Event] and aggregates their fully qualified names
 * into a resource file located at [OUTPUT_PATH] (META-INF/orbit/events).
 *
 * It is designed to serve as a fallback or primary processor for Java-based projects or environments
 * where KSP is not used. It adheres to the standard JSR-269 API.
 */
class EventAnnotationProcessor : AbstractProcessor() {
    private val collectedEvents = mutableSetOf<String>()

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedAnnotationTypes(): MutableSet<String> = mutableSetOf(Event::class.java.canonicalName)

    /**
     * Processes the annotations in the current round.
     *
     * Collects [Event] annotated elements in each round and writes the accumulated list
     * to the output file in the final processing round.
     *
     * @param annotations The annotation types requested to be processed.
     * @param roundEnv Environment for information about the current and prior round.
     * @return false, allowing other processors to also process the [Event] annotation.
     */
    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment,
    ): Boolean {
        // Collect all events in this round
        val elements = roundEnv.getElementsAnnotatedWith(Event::class.java)
        elements
            .filterIsInstance<TypeElement>()
            .forEach { typeElement ->
                collectedEvents.add(typeElement.qualifiedName.toString())
            }

        // Write the file only when processing is over (in the last round)
        if (roundEnv.processingOver() && collectedEvents.isNotEmpty()) {
            try {
                val filer = processingEnv.filer
                val resource =
                    filer.createResource(
                        StandardLocation.CLASS_OUTPUT,
                        "",
                        OUTPUT_PATH,
                    )
                resource.openOutputStream().use { outputStream ->
                    writeEventsFile(outputStream, collectedEvents)
                }
            } catch (e: Exception) {
                processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Failed to write events file: ${e.message}",
                )
            }
        }

        return false // We do not claim the annotation, other processors might want to use it too
    }
}
