package io.orbit.core.service

import kotlin.uuid.Uuid

@JvmInline
value class ServiceName(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "ServiceName cannot be blank" }
        require(value.matches(NAME_PATTERN)) {
            "ServiceName must contain only lowercase letters, numbers, and hyphens"
        }
    }

    companion object {
        private val NAME_PATTERN = "[a-z0-9-]+".toRegex()
    }
}

@JvmInline
value class ServiceId(
    val value: String,
) {
    init {
        require(value.matches(UUID_PATTERN)) {
            "ServiceId must be a valid UUID"
        }
    }

    companion object {
        fun random(): ServiceId = ServiceId(Uuid.random().toString())

        private val UUID_PATTERN =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}".toRegex()
    }
}

data class ServiceIdentity(
    val name: ServiceName,
    val id: ServiceId,
) {
    val source: String
        get() = "${name.value}-${id.value}"

    companion object {
        operator fun invoke(name: String): ServiceIdentity =
            ServiceIdentity(
                name = ServiceName(name),
                id = ServiceId.random(),
            )
    }
}
