pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        id("org.openapi.generator") version "7.3.0"
    }
}

rootProject.name = "banking-api"

include(":openapi-spec")
include(":homework-1")
