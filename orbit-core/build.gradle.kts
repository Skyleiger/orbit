plugins {
    alias(libs.plugins.orbit.kotlin.conventions)
    alias(libs.plugins.buildconfig)
}

dependencies {
    api(libs.kotlinx.coroutines)
    implementation(kotlin("reflect"))
}

buildConfig {
    packageName("io.orbit.core")
    className("OrbitBuildConfig")
    buildConfigField("REVISION", providers.gradleProperty("revision").orElse("orbit"))
    buildConfigField("VERSION", project.version.toString())
}
