plugins {
  // Preload shared Kotlin Gradle plugins once via version catalog to ensure a single classloader instance
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.plugin.serialization) apply false
  alias(libs.plugins.kotlin.plugin.spring) apply false

  alias(libs.plugins.versions)
}