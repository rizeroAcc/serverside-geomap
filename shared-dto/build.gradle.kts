plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

group = "com.server"
version = "0.0.1"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(21)
    jvm()
    val xcfName = "shared-dtoKit"

    iosX64 {
        binaries.framework { baseName = xcfName }
    }

    iosArm64 {
        binaries.framework { baseName = xcfName }
    }

    iosSimulatorArm64 {
        binaries.framework { baseName = xcfName }
    }
    sourceSets{
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            }
        }
    }
}