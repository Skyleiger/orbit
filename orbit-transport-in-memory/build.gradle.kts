plugins {
    alias(libs.plugins.orbit.kotlin.conventions)
}

dependencies {
    api(projects.orbitCore)

    testImplementation(testFixtures(projects.orbitCore))
}
