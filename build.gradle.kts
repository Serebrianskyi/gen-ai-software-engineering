plugins {
    kotlin("jvm") version "1.9.23" apply false
    kotlin("plugin.spring") version "1.9.23" apply false
    id("org.springframework.boot") version "3.2.1" apply false
    id("io.spring.dependency-management") version "1.1.4"
    id("com.diffplug.spotless") version "6.25.0"
}

repositories {
    mavenCentral()
}

spotless {
    kotlin {
        target("homework-1/src/main/**/*.kt")
        ktlint("0.50.0")
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint("0.50.0")
    }
}

subprojects {
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "com.diffplug.spotless")

    spotless {
        kotlin {
            target("src/main/**/*.kt")
            ktlint("0.50.0")
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            ktlint("0.50.0")
        }
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.2.1")
        }
    }
}
