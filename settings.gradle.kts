rootProject.name = "orbit"

// Plugin repositories configuration
pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

// Dependency repositories configuration
dependencyResolutionManagement {
  @Suppress("UnstableApiUsage")
  repositories {
    mavenCentral()
  }
}

// Enable build cache
buildCache {
  local {
    isEnabled = true
  }
}

// Include build logic
includeBuild("build-logic")

// Enable type-safe project accessors
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Subproject configuration
include(":orbit-core")
include(":orbit-transport-in-memory")
include(":orbit-transport-rabbitmq")
include(":orbit-serialization-jackson")
include(":orbit-serialization-kotlinx")
include(":orbit-spring-boot-starter")