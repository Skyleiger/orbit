plugins {
    alias(libs.plugins.orbit.kotlin.conventions)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.plugin.spring)
}

dependencies {
    api(projects.orbitCore)
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.autoconfigure)
    annotationProcessor(libs.spring.boot.configuration.processor)
}