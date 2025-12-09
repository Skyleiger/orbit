package io.orbit.serialization.kotlinx

import io.orbit.core.serializer.SerializerFactory
import io.orbit.core.service.ServiceIdentity
import kotlinx.serialization.json.Json

/**
 * Factory for creating [KotlinxEventSerializer] instances.
 *
 * @param json The kotlinx.serialization Json instance to use for serialization.
 *             If not provided, a default Json configuration will be created.
 */
class KotlinxSerializerFactory(
    private val json: Json? = null,
) : SerializerFactory {
    override fun create(serviceIdentity: ServiceIdentity) =
        if (json != null) {
            KotlinxEventSerializer(json)
        } else {
            KotlinxEventSerializer()
        }
}
