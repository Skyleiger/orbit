plugins {
  kotlin("jvm")
  id("org.jlleitschuh.gradle.ktlint")
}

kotlin {
  jvmToolchain(21)

  compilerOptions {
    optIn.add("kotlin.time.ExperimentalTime")
  }
}

ktlint {
  verbose.set(true)
  outputToConsole.set(true)
}