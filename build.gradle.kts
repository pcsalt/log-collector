plugins {
  id("org.springframework.boot") version "3.4.1"
  id("io.spring.dependency-management") version "1.1.7"
  kotlin("jvm") version "2.3.0"
  kotlin("plugin.spring") version "2.3.0"
  `maven-publish`
}

group = "com.pcsalt"
version = "1.1.0"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
  withSourcesJar()
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

repositories {
  mavenCentral()
}

dependencies {
  // Spring Boot
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-websocket")
  implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
  implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-security")

  // Kotlin
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")

  // SQLite
  implementation("org.xerial:sqlite-jdbc:3.47.1.0")

  // Testing
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("io.mockk:mockk:1.14.9")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.bootJar {
  archiveFileName.set("log-collector.jar")
}

// Client JAR - contains only the appender classes
val clientJar by tasks.registering(Jar::class) {
  archiveBaseName.set("log-collector-client")
  archiveClassifier.set("")
  from(sourceSets.main.get().output) {
    include("com/pcsalt/logcollector/client/**")
  }
}

// Maven publishing - only client library (appenders)
publishing {
  publications {
    create<MavenPublication>("client") {
      artifactId = "log-collector-client"
      artifact(clientJar) {
        classifier = null
      }

      pom {
        name.set("Log Collector Client")
        description.set("HTTP Logback appender for log-collector service")
      }
    }
  }

  repositories {
    mavenLocal()
  }
}
