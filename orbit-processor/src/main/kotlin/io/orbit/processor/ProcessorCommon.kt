package io.orbit.processor

import java.io.OutputStream

/**
 * The output path for the generated events file.
 *
 * The file will contain discovered events. Each line contains the fully qualified class name of a discovered event.
 */
const val OUTPUT_PATH = "META-INF/orbit/events"

/**
 * This function writes the collection of event class names to the provided output stream,
 * ensuring each name is on a new line.
 *
 * @param outputStream The stream to write to.
 * @param events The collection of fully qualified event class names.
 */
fun writeEventsFile(
    outputStream: OutputStream,
    events: Collection<String>,
) {
    outputStream.bufferedWriter().use { writer ->
        events.forEach { eventName ->
            writer.appendLine(eventName)
        }
    }
}
