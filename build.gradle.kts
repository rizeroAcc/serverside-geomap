plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.mapprjct"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin{
    compilerOptions {
        jvmToolchain(24)
    }
}

dependencies {
    implementation(project(":shared-dto"))
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.postgresql)
    implementation(libs.h2)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.datetime)
    implementation(libs.exposed.dao)

    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    implementation("io.ktor:ktor-serialization-gson:3.3.2")
    implementation("io.ktor:ktor-server-sessions:3.3.2")
    implementation("io.ktor:ktor-server-partial-content:3.3.2")
    implementation("io.ktor:ktor-server-status-pages:3.3.2")
    implementation("io.arrow-kt:arrow-core:2.2.1.1")
    implementation("io.arrow-kt:arrow-fx-coroutines:2.2.1.1")
    testImplementation("io.mockk:mockk:1.14.9")
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.assertj.core)
    testImplementation(libs.koin.test.junit5)
    testImplementation(libs.koin.kotest)


    testImplementation(libs.kotest.testcontainers)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.ktor)

    testImplementation("io.ktor:ktor-client-content-negotiation:3.3.2")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.3")
}
