plugins {
  kotlin("jvm")
}

kotlin {
  jvmToolchain(21)

  compilerOptions {
    optIn.add("kotlin.time.ExperimentalTime")
  }
}