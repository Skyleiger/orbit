plugins {
    alias(libs.plugins.orbit.kotlin.conventions)
    alias(libs.plugins.kotlin.plugin.serialization)
}

dependencies {
    // Dependencies to modules we want to test
    testImplementation(projects.orbitCore)
    testImplementation(projects.orbitTransportInMemory)
    testImplementation(projects.orbitTransportRabbitmq)
    testImplementation(projects.orbitSerializationJackson)
    testImplementation(projects.orbitSerializationKotlinx)

    // Test frameworks
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.kotlinx.coroutines)
    testImplementation(libs.bundles.jackson)
    testImplementation(libs.testcontainers.rabbitmq)
    testRuntimeOnly(libs.slf4j.simple)
}
