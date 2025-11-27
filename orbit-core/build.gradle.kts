plugins {
    alias(libs.plugins.orbit.kotlin.conventions)
}

dependencies {
    api(libs.kotlinx.coroutines)
    implementation(kotlin("reflect"))
}
