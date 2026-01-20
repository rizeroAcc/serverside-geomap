package com.mapprjct.junit.controller

import com.mapprjct.ApplicationStartMode
import com.mapprjct.database.tables.*
import com.mapprjct.model.dto.User
import com.mapprjct.model.request.RegistrationRequest
import com.mapprjct.model.request.SignInRequest
import com.mapprjct.model.response.RegistrationResponse
import com.mapprjct.module
import com.mapprjct.service.UserService
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.*
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.ktor.ext.getKoin
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthenticationControllerTest{

    fun testKtorApp(
        block: suspend ApplicationTestBuilder.() -> Unit
    ) = testApplication {
        environment {
            config = ApplicationConfig("application-test.yaml")
        }
        client = client.config {
            install(ContentNegotiation) { json() }
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
        startApplication()
        block()
    }

    companion object {
        @Container
        val postgreSQLContainer = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testDb")
            .withUsername("postgres")
            .withPassword("test")
            .withReuse(true)
    }

    private lateinit var database : Database
    @BeforeAll
    fun initialize() {
        database = Database.connect(
            url = postgreSQLContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        )
    }

    @AfterAll
    fun shutdown() {
        stopKoin()
    }

    @BeforeEach
    fun setUp() {
        transaction(database) {
            SchemaUtils.drop(
                UserTable,
                SessionTable,
                ProjectTable,
                ProjectUsersTable,
                InviteCodeTable
            )
        }
    }

    @Nested
    inner class Registration{
        @Test
        fun `should register new user in system`() = testKtorApp {
            val userService = this.application.getKoin().get<UserService>()
            val userForRegistration = User("89036559989","kirill")
            val userForRegistrationPassword = "testPassword"
            val registrationRequest = RegistrationRequest(
                phone = userForRegistration.phone,
                username = userForRegistration.username,
                password = userForRegistrationPassword
            )

            val responseBody = client.post("/register") {
                contentType(ContentType.Application.Json)
                setBody(registrationRequest)
            }.body<RegistrationResponse>()
            val awaitedResponse = RegistrationResponse(userForRegistration)
            assertThat(responseBody)
                .isEqualTo(awaitedResponse)
            //check user created
            val savedUser = userService.getUser(userForRegistration.phone).getOrThrow()
            assertThat(savedUser)
                .isEqualTo(userForRegistration)
        }
        @Test
        fun `should respond 409 if user already registered`() = testKtorApp {
            val userForRegistration = User("89036559989","kirill")
            val userForRegistrationPassword = "testPassword"
            val registrationRequest = RegistrationRequest(
                phone = userForRegistration.phone,
                username = userForRegistration.username,
                password = userForRegistrationPassword
            )

            client.post("/register") {
                contentType(ContentType.Application.Json)
                setBody(registrationRequest)
            }
            val response = client.post("/register") {
                contentType(ContentType.Application.Json)
                setBody(registrationRequest)
            }
            assertThat(response.status)
                .isEqualTo(HttpStatusCode.Conflict)
        }
        @Test
        fun `should respond 400 if registration data invalid`() = testKtorApp {
            val userForRegistration = User("89036559989", "")
            val userForRegistrationPassword = "testPassword"
            val registrationRequest = RegistrationRequest(
                phone = userForRegistration.phone,
                username = userForRegistration.username,
                password = userForRegistrationPassword
            )

            val response = client.post("/register") {
                contentType(ContentType.Application.Json)
                setBody(registrationRequest)
            }
            assertThat(response.status)
                .isEqualTo(HttpStatusCode.BadRequest)
        }
    }

    @Nested
    inner class Authorization{
        @Test
        fun `should authorize user`() = testKtorApp {
            val sessionStorage = this.application.getKoin().get<SessionStorage>()
            val user = User("89036559989","kirill")
            val userPassword = "testPassword"
            val registrationRequest = RegistrationRequest(
                phone = user.phone,
                username = user.username,
                password = userPassword
            )

            client.post("/register") {
                contentType(ContentType.Application.Json)
                setBody(registrationRequest)
            }

            val signInRequest = SignInRequest(
                phone = user.phone,
                password = userPassword
            )

            val response = client.post("/signin") {
                contentType(ContentType.Application.Json)
                setBody(signInRequest)
            }
            assertThat(response.status)
                .isEqualTo(HttpStatusCode.OK)
            val responseToken = response.headers["Authorization"]!!
            val readSession = runCatching {
                sessionStorage.read(responseToken)
            }
            assertThat(readSession.isSuccess)
        }
        @Test
        fun `should respond 401 if credentials invalid`() = testKtorApp {
            val signInRequest = SignInRequest(
                phone = "89036559989",
                password = "userPassword"
            )

            val response = client.post("/signin") {
                contentType(ContentType.Application.Json)
                setBody(signInRequest)
            }
            assertThat(response.status)
                .isEqualTo(HttpStatusCode.Unauthorized)
        }
    }

    @Nested
    inner class LogOut{
        @Test
        fun `should clear user sessions`() = testKtorApp {
            val sessionStorage = this.application.getKoin().get<SessionStorage>()
            val user = User("89036559989","kirill")
            val userPassword = "testPassword"
            val registrationRequest = RegistrationRequest(
                phone = user.phone,
                username = user.username,
                password = userPassword
            )
            val signInRequest = SignInRequest(
                phone = user.phone,
                password = userPassword
            )
            client.post("/register") {
                contentType(ContentType.Application.Json)
                setBody(registrationRequest)
            }
            val token = client.post("/signin") {
                contentType(ContentType.Application.Json)
                setBody(signInRequest)
            }.headers["Authorization"]!!

            val response = client.post("/logout") {
                contentType(ContentType.Application.Json)
                headers.append("Authorization", token)
            }
            assertThat(response.status)
                .isEqualTo(HttpStatusCode.Accepted)
            //check session cleared
            val readToken = runCatching {
                sessionStorage.read(token)
            }
            assertThat(readToken.exceptionOrNull())
                .isInstanceOf(NoSuchElementException::class.java)
        }
        @Test
        fun `should answer 404 if session invalid`() = testKtorApp {
            val response = client.post("/logout") {
                headers.append("Authorization", "token")
            }
            assertThat(response.status)
                .isEqualTo(HttpStatusCode.NotFound)
        }
        @Test
        fun `should answer 204 if session isn't set`() = testKtorApp {
            val response = client.post("/logout") {}
            assertThat(response.status)
                .isEqualTo(HttpStatusCode.NoContent)
        }
    }
}