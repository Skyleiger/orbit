plugins {
  alias(libs.plugins.orbit.kotlin.conventions)
  alias(libs.plugins.kotlin.plugin.serialization)
  alias(libs.plugins.orbit.kotest.conventions)
}

dependencies {
  api(projects.orbitCore)
  implementation(libs.kotlinx.serialization)
  implementation(kotlin("reflect"))
}