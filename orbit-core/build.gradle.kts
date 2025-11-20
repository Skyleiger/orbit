plugins {
    alias(libs.plugins.orbit.kotlin.conventions)
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(kotlin("reflect"))
}
