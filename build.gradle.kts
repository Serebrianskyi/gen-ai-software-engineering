plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.springdoc.openapi-gradle-plugin") version "1.8.0"
    id("com.diffplug.spotless") version "6.25.0"
}

// API Spec Version
val apiSpecVersion = "0.0.1"

group = "com.banking"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // OpenAPI & Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.3.0")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-Xjsr305=strict"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

springBoot {
    mainClass.set("com.banking.ApplicationKt")
}

// Spotless code formatting
spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("0.50.0").setEditorConfigPath(rootProject.file(".editorconfig"))
    }
    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        ktlint("0.50.0").setEditorConfigPath(rootProject.file(".editorconfig"))
    }
}

// Make build check formatting before tests
tasks.named("check") {
    dependsOn("spotlessCheck")
}
