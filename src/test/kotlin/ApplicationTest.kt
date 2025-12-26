package com.mapprjct

import com.mapprjct.model.request.RegistrationRequest
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationTest {

    companion object {
        @Container
        val postgreSQLContainer = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testDb")
            .withUsername("postgres")
            .withPassword("test")
            .withReuse(true)
    }

    @BeforeAll
    fun initialize() {

    }

    @Test
    fun `should register new user in system`() = testApplication {
        client = client.config {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }
        application {
            module(
                startMode = ApplicationStartMode.TEST(
                    dbURL = postgreSQLContainer.jdbcUrl,
                    dbUsername = postgreSQLContainer.username,
                    dbPassword = postgreSQLContainer.password
                )
            )
        }

        val registrationRequest = RegistrationRequest(
            phone = "89036559989",
            username = "Kirill",
            password = "testPassword"
        )

        val response = client.post("/register"){
            contentType(ContentType.Application.Json)
            setBody(registrationRequest)
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

}
