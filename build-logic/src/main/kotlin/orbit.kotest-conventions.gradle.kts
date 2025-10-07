import com.adarshr.gradle.testlogger.theme.ThemeType

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    id("orbit.kotlin-conventions")
    id("com.adarshr.test-logger")
}

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
