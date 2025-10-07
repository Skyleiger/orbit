import gradle.kotlin.dsl.accessors._0fc522749a1d24f2e63b2000ed6035a6.detekt

plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
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

detekt {
    ignoreFailures = true
}
