package io.orbit.processor.ksp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import io.orbit.core.event.Event
import io.orbit.processor.OUTPUT_PATH
import io.orbit.processor.writeEventsFile
import java.io.IOException

/**
 * Kotlin Symbol Processing (KSP) implementation of the Orbit Event Processor.
 *
 * This processor scans for all classes annotated with [Event] and aggregates their fully qualified names
 * into a resource file located at [OUTPUT_PATH] (META-INF/orbit/events).
 *
 * It is designed to work with Kotlin source code and leverages KSP's performance benefits over KAPT.
 */
class EventSymbolProcessor(
    private val environment: SymbolProcessorEnvironment,
) : SymbolProcessor {
    private var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // We use a boolean flag to ensure we process the symbols only once.
        // Since we are scanning the source code for existing classes and not waiting for
        // generated classes from other processors, doing this in the first round is enough.
        if (invoked) {
            return emptyList()
        }

        invoked = true

        val eventAnnotationName = Event::class.qualifiedName
        if (eventAnnotationName == null) {
            environment.logger.warn("Event annotation class could not be resolved (qualifiedName is null). No events will be processed.")
            return emptyList()
        }

        val symbols = resolver.getSymbolsWithAnnotation(eventAnnotationName)

        // Collect all events in this round (only concrete classes, no interfaces, enums, or abstract classes)
        // We ignore validation because we only need the class name, which is available even if the class is technically invalid.
        val events =
            symbols
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.CLASS && !it.modifiers.contains(Modifier.ABSTRACT) }
                .toList()

        if (events.isEmpty()) {
            return emptyList()
        }

        // We collect all sources for aggregation.
        // This tells KSP that the output depends on these input files.
        val dependencies =
            Dependencies(
                aggregating = true,
                sources = events.mapNotNull { it.containingFile }.toTypedArray(),
            )

        try {
            val resourceFile =
                environment.codeGenerator.createNewFile(
                    dependencies = dependencies,
                    packageName = "", // META-INF is not a package
                    fileName = OUTPUT_PATH,
                    extensionName = "",
                )

            resourceFile.use { outputStream ->
                val eventNames = events.mapNotNull { it.qualifiedName?.asString() }
                writeEventsFile(outputStream, eventNames)
            }
        } catch (e: IOException) {
            environment.logger.error("Failed to write events file: ${e.message}")
        }

        // We return an empty list because we have processed all symbols we care about.
        // We don't want to defer any symbols to the next round.
        return emptyList()
    }
}
