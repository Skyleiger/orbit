# orbit

### A modular, transport-independent messaging toolkit for the JVM (Kotlin/Java)

---

![Build Status](https://img.shields.io/badge/build-unknown-lightgrey)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-blue.svg?logo=kotlin)
![Java](https://img.shields.io/badge/Java-21-orange.svg?logo=openjdk)
![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)

<!-- ![Build](https://github.com/Skyleiger/orbit/actions/workflows/build.yml/badge.svg) -->

orbit simplifies event-driven communication in distributed applications. It acts as an abstraction layer between your services and the messaging infrastructure, allowing you
to focus on business logic rather than transport details.

**Key characteristics:**

- 🎯 **Event-driven architecture** - Services communicate through well-defined, typesafe events
- 🔌 **Transport-agnostic** - Switch between RabbitMQ, in-memory transport, or bring your own
- 📦 **Pluggable serialization** - Choose Jackson, kotlinx.serialization, or bring your own
- 🧪 **Built for testing** - In-memory transport for fast, reliable unit and integration tests
- 🚀 **Spring Boot ready** - Auto-configuration and custom features for seamless integration

> **Note:** orbit is in early development and not yet ready for production use.

---

## Getting Started

### Requirements

- Java 21 or higher
- Kotlin 2.2 or higher (for Kotlin projects)

### Dependencies

orbit follows a modular architecture. To use it, you need to add:

1. **Core or Spring Boot Starter**
   - `orbit-core` - Standalone usage
   - `orbit-spring-boot-starter` - Spring Boot integration (includes `orbit-core`)

2. **One Transport**
   - `orbit-transport-in-memory` - In-memory transport (for development/testing)
   - `orbit-transport-rabbitmq` - RabbitMQ transport (AMQP)

3. **One Serialization**
   - `orbit-serialization-jackson` - JSON serialization with Jackson
   - `orbit-serialization-kotlinx` - JSON serialization with kotlinx.serialization

4. **Optional: Annotation Processor**
   - `orbit-processor` - Compile-time processor that discovers `@Event`-annotated classes and adds metadata to the classpath for automatic event registration

### Using the BOM

To ensure version compatibility across all `orbit` modules, we provide a Bill of Materials (BOM).

**Gradle (Kotlin DSL):**

```kotlin
dependencies {
  // Import the BOM to manage versions
  implementation(platform("io.orbit:orbit-bom:0.1.0-SNAPSHOT"))

  // Core modules (choose one)
  implementation("io.orbit:orbit-core")                  // Standalone
  implementation("io.orbit:orbit-spring-boot-starter")   // Spring Boot

  // Transport modules (choose one)
  implementation("io.orbit:orbit-transport-in-memory")   // In-memory
  implementation("io.orbit:orbit-transport-rabbitmq")    // RabbitMQ

  // Serialization modules (choose one)
  implementation("io.orbit:orbit-serialization-jackson") // Jackson
  implementation("io.orbit:orbit-serialization-kotlinx") // kotlinx.serialization

  // Optional: Annotation processor
  ksp("io.orbit:orbit-processor")                        // KSP (Kotlin)
  annotationProcessor("io.orbit:orbit-processor")        // APT (Java)
}
```

**Maven:**

```xml
<dependencyManagement>
  <dependencies>
    <!-- Import the BOM to manage versions -->
    <dependency>
      <groupId>io.orbit</groupId>
      <artifactId>orbit-bom</artifactId>
      <version>0.1.0-SNAPSHOT</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <!-- Core modules (choose one) -->
  <dependency>
    <groupId>io.orbit</groupId>
    <artifactId>orbit-core</artifactId> <!-- Standalone -->
  </dependency>
  <dependency>
    <groupId>io.orbit</groupId>
    <artifactId>orbit-spring-boot-starter</artifactId> <!-- Spring Boot -->
  </dependency>

  <!-- Transport modules (choose one) -->
  <dependency>
    <groupId>io.orbit</groupId>
    <artifactId>orbit-transport-in-memory</artifactId> <!-- In-memory -->
  </dependency>
  <dependency>
    <groupId>io.orbit</groupId>
    <artifactId>orbit-transport-rabbitmq</artifactId> <!-- RabbitMQ -->
  </dependency>

  <!-- Serialization modules (choose one) -->
  <dependency>
    <groupId>io.orbit</groupId>
    <artifactId>orbit-serialization-jackson</artifactId> <!-- Jackson -->
  </dependency>
  <dependency>
    <groupId>io.orbit</groupId>
    <artifactId>orbit-serialization-kotlinx</artifactId> <!-- kotlinx.serialization -->
  </dependency>
</dependencies>

<!-- Optional: Annotation processor -->
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <configuration>
        <annotationProcessorPaths>
          <path>
            <groupId>io.orbit</groupId>
            <artifactId>orbit-processor</artifactId>
          </path>
        </annotationProcessorPaths>
      </configuration>
    </plugin>
  </plugins>
</build>
```

---

## Building from Source

This project uses the Gradle Wrapper, which is the recommended way to build the project. The wrapper ensures a consistent build environment by using the specific Gradle
version defined in the project, so you do not need to install Gradle manually.

To build the project locally, run the appropriate command for your operating system from the root directory:

**Linux / macOS:**

```bash
./gradlew build
```

**Windows:**

```bash
gradlew.bat build
```

## License

This project is licensed under the Apache License, Version 2.0.
See the [LICENSE](LICENSE) file for the full text and the [NOTICE](NOTICE) file for attribution and third‑party
notices.
