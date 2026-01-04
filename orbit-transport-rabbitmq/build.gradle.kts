plugins {
    alias(libs.plugins.orbit.kotlin.conventions)
}

dependencies {
    api(projects.orbitCore)

    implementation(libs.rabbitmq.client)
    implementation(libs.kotlinx.coroutines)

    testImplementation(testFixtures(projects.orbitCore))
    testImplementation(libs.testcontainers.rabbitmq)
    testRuntimeOnly(libs.slf4j.simple)
}
