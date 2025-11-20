plugins {
    `java-platform`
}

dependencies {
    constraints {
        rootProject.subprojects
            .filter { it != project }
            .filterNot { it.path == projects.orbitTests.path }
            .forEach {
                api(it)
            }
    }
}
