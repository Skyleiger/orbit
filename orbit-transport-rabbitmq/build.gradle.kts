plugins {
    alias(libs.plugins.orbit.kotlin.conventions)
}

dependencies {
    api(projects.orbitCore)

    implementation(libs.rabbitmq.client)
    implementation(libs.kotlinx.coroutines)
}
