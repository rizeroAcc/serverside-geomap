package com.mapprjct.controller

import com.mapprjct.ApplicationStartMode
import com.mapprjct.database.daoimpl.SessionRepositoryImpl
import com.mapprjct.database.repositoryImpl.UserRepositoryImpl
import com.mapprjct.database.storage.PostgresSessionStorage
import com.mapprjct.database.tables.InviteCodeTable
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.database.tables.SessionTable
import com.mapprjct.database.tables.UserTable
import com.mapprjct.model.dto.User
import com.mapprjct.model.request.RegistrationRequest
import com.mapprjct.model.request.SignInRequest
import com.mapprjct.model.response.RegistrationResponse
import com.mapprjct.module
import com.mapprjct.service.UserService
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.sessions.SessionStorage
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthenticationControllerTest{

    fun testKtorApp(
        block: suspend ApplicationTestBuilder.() -> Unit
    ) = testApplication {
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
    private lateinit var userService : UserService
    private lateinit var sessionStorage : SessionStorage
    @BeforeAll
    fun initialize() {
        database = Database.connect(
            url = postgreSQLContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        )
        userService = UserService(
            userRepository = UserRepositoryImpl(database),
            database = database
        )
        sessionStorage = PostgresSessionStorage(SessionRepositoryImpl(database))
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

    @Test
    fun `should register new user in system`() = testKtorApp {
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
    @Test
    fun `should authorize user`() = testKtorApp {
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
    @Test
    fun `should clear user sessions`() = testKtorApp {
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
            headers.append("Authorization", "token")
        }
        assertThat(response.status)
            .isEqualTo(HttpStatusCode.Accepted)
        //check session cleared
        val readedToken = runCatching {
            sessionStorage.read(token)
        }
        assertThat(readedToken.exceptionOrNull())
            .isInstanceOf(NoSuchElementException::class.java)
    }
    @Test
    fun `should answer 404 if session invalid`() = testKtorApp {
        val response = client.post("/logout") {
            headers.append("Authorization", "token")
        }
        assertThat(response.status)
            .isEqualTo(HttpStatusCode.Unauthorized)
    }
    @Test
    fun `should answer 204 if session invalid`() = testKtorApp {
        val response = client.post("/logout") {}
        assertThat(response.status)
            .isEqualTo(HttpStatusCode.NoContent)
    }
}