plugins {
    alias(libs.plugins.orbit.kotlin.conventions)
    alias(libs.plugins.kotlin.plugin.serialization)
}

dependencies {
    api(projects.orbitCore)
    implementation(libs.kotlinx.serialization.json)
    implementation(kotlin("reflect"))
}
