package io.orbit.serialization.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import io.orbit.core.serializer.SerializerFactory
import io.orbit.core.service.ServiceIdentity

/**
 * Factory for creating [JacksonEventSerializer] instances.
 *
 * @param objectMapper The Jackson ObjectMapper to use for serialization.
 *                     If not provided, a default mapper will be created.
 */
class JacksonSerializerFactory(
    private val objectMapper: ObjectMapper? = null,
) : SerializerFactory {
    override fun create(serviceIdentity: ServiceIdentity) =
        if (objectMapper != null) {
            JacksonEventSerializer(objectMapper)
        } else {
            JacksonEventSerializer()
        }
}
