plugins {
    alias(libs.plugins.orbit.kotlin.conventions)
}

dependencies {
    api(projects.orbitCore)
    implementation(libs.bundles.jackson)
}
