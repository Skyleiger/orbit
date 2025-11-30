plugins {
    alias(libs.plugins.orbit.kotlin.conventions)
}

dependencies {
    implementation(projects.orbitCore)
    implementation(libs.ksp.api)

    testImplementation(libs.compile.testing)
}
