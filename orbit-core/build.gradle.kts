plugins {
  alias(libs.plugins.orbit.kotlin.conventions)
  alias(libs.plugins.orbit.kotest.conventions)
}

dependencies {
  implementation(libs.kotlinx.coroutines)
}