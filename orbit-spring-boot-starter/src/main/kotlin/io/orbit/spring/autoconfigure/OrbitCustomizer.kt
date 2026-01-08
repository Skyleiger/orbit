package io.orbit.spring.autoconfigure

import io.orbit.core.Orbit
import io.orbit.core.OrbitBuilder

/**
 * Interface to customize the [OrbitBuilder] before the [Orbit] is created.
 *
 * This allows adding custom configuration, registering additional event handlers,
 * or modifying the builder state without having to override the entire Orbit bean definition.
 */
fun interface OrbitCustomizer {
    /**
     * Customize the [OrbitBuilder].
     *
     * @param builder the builder to customize
     */
    fun customize(builder: OrbitBuilder)
}
