pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        id("org.openapi.generator") version "7.3.0"
    }
}
rootProject.name = "ai-workshops"
include(":openapi-spec")
include(":homework-1")
include("homework-2")
