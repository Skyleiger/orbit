import com.adarshr.gradle.testlogger.theme.ThemeType
import io.gitlab.arturbosch.detekt.Detekt

plugins {
    kotlin("jvm")
    id("com.adarshr.test-logger")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
}

configureKotlin()
configureKotest()
configureKtlint()
configureDetekt()

fun Project.configureKotlin() {
    kotlin {
        jvmToolchain(21)

        compilerOptions {
            optIn.add("kotlin.time.ExperimentalTime")
            optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
            optIn.add("kotlin.uuid.ExperimentalUuidApi")
            optIn.add("kotlin.concurrent.atomics.ExperimentalAtomicApi")
        }
    }
}

fun Project.configureKotest() {
    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

    dependencies {
        testImplementation(kotlin("test"))
        // Dynamische Nutzung des Version Catalogs über das Extension-Objekt
        testImplementation(libs.findBundle("kotest").get())
    }

    tasks.test {
        useJUnitPlatform()
    }

    testlogger {
        theme = ThemeType.MOCHA_PARALLEL
        showSummary = true
        showSkipped = true
        showFullStackTraces = true
        showPassed = true
        showFailed = true
    }
}

fun Project.configureKtlint() {
    ktlint {
        verbose.set(true)
        outputToConsole.set(true)
    }
}

fun Project.configureDetekt() {
    tasks.withType<Detekt>().configureEach {
        enabled = false
    }

    detekt {
        ignoreFailures = true
    }
}
