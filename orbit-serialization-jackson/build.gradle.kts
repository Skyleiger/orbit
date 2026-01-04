plugins {
    alias(libs.plugins.orbit.kotlin.conventions)
}

dependencies {
    api(projects.orbitCore)
    api(libs.bundles.jackson)

    testImplementation(testFixtures(projects.orbitCore))
}
