plugins {
    id("orbit.kotlin-conventions")
}

dependencies {
    implementation(projects.orbitCore)
    implementation(libs.ksp.api)
}
