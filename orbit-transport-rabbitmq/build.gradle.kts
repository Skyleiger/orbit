plugins {
  alias(libs.plugins.orbit.kotlin.conventions)
  alias(libs.plugins.orbit.kotest.conventions)
}

dependencies {
  api(projects.orbitCore)
}