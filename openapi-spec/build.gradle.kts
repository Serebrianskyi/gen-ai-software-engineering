plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.openapi.generator")
    `java-library`
    `maven-publish`
}

group = "com.banking"
version = "0.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    api("org.springframework.boot:spring-boot-starter-web:3.2.3")
    api("org.springframework.boot:spring-boot-starter-validation:3.2.3")

    // OpenAPI & Swagger
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    api("org.springdoc:springdoc-openapi-starter-webmvc-api:2.3.0")

    // Jakarta Bean Validation
    api("jakarta.validation:jakarta.validation-api:3.0.2")

    // Jackson
    api("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")

    // Kotlin
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22")
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("${rootProject.rootDir}/openapi.yaml")
    outputDir.set("$buildDir/generated/openapi")
    apiPackage.set("com.banking.api")
    modelPackage.set("com.banking.model.generated")
    packageName.set("com.banking.generated")
    configOptions.set(mapOf(
        "useSpringBoot3" to "true",
        "documentationProvider" to "springdoc",
        "skipDefaultInterface" to "true",
        "interfaceOnly" to "false",
        "useBeanValidation" to "true",
        "packageVersion" to "0.0.1",
        "openApiNullable" to "false",
        "enumPropertyNaming" to "UPPERCASE",
        "parcelableModels" to "false",
        "serializableModel" to "true"
    ))
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

sourceSets {
    main {
        kotlin {
            srcDir("$buildDir/generated/openapi/src/main/kotlin")
        }
    }
}

tasks.compileKotlin {
    dependsOn("openApiGenerate")
}

tasks.jar {
    dependsOn("openApiGenerate")
}