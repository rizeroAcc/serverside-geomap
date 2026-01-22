package com.mapprjct

import com.mapprjct.module
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.testcontainers.containers.JdbcDatabaseContainer

fun testKtorApp(
    container: JdbcDatabaseContainer<*>,
    block: suspend ApplicationTestBuilder.() -> Unit
) = testApplication {
    environment {
        config = ApplicationConfig("application-test.yaml")
    }
    client = client.config {
        install(ContentNegotiation) { json() }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }
    application {
        module(
            startMode = ApplicationStartMode.TEST(
                dbURL = container.jdbcUrl,
                dbUsername = container.username,
                dbPassword = container.password
            )
        )
    }
    startApplication()
    block()
}