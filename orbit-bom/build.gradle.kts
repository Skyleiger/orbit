plugins {
  `java-platform`
}

dependencies {
  constraints {
    rootProject.subprojects.filter { it != project }.forEach {
      api(it)
    }
  }
}