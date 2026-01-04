plugins {
    alias(libs.plugins.orbit.kotlin.conventions)
    alias(libs.plugins.buildconfig)
    `java-test-fixtures`
}

dependencies {
    api(libs.kotlinx.coroutines)
    implementation(kotlin("reflect"))

    testFixturesApi(libs.kotest.runner.junit5)
    testFixturesApi(libs.kotest.assertions.core)
}

buildConfig {
    packageName("io.orbit.core")
    className("OrbitBuildConfig")
    buildConfigField("REVISION", providers.gradleProperty("revision").orElse("orbit"))
    buildConfigField("VERSION", project.version.toString())
}
