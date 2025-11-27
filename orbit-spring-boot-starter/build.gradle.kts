plugins {
    alias(libs.plugins.orbit.kotlin.conventions)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.plugin.spring)
}

dependencies {
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.autoconfigure)
    annotationProcessor(libs.spring.boot.configuration.processor)

    api(projects.orbitCore)
    compileOnly(projects.orbitTransportInMemory)
    compileOnly(projects.orbitTransportRabbitmq)
    compileOnly(projects.orbitSerializationJackson)
    compileOnly(projects.orbitSerializationKotlinx)
}

tasks.named("bootJar") {
    enabled = false
}
