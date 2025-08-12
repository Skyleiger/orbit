plugins {
  `kotlin-dsl`
}

kotlin {
  jvmToolchain(21)
}

dependencies {
  implementation(libs.kotlin.gradle.plugin)
  implementation(libs.test.logger.plugin)
}