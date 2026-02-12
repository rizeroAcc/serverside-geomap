pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()           // если нужны androidx/google вещи
    }
    plugins {
        id("org.jetbrains.kotlin.multiplatform") version "2.3.0"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}