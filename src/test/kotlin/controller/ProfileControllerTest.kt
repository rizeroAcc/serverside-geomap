package com.mapprjct.controller

import com.mapprjct.AppConfig
import com.mapprjct.ApplicationStartMode
import com.mapprjct.model.dto.User
import com.mapprjct.model.request.RegistrationRequest
import com.mapprjct.model.request.SignInRequest
import com.mapprjct.model.response.AvatarUpdateResponse
import com.mapprjct.module
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import io.ktor.utils.io.toByteArray
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.ktor.ext.getKoin
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class ProfileControllerTest {
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
    fun setUp(){
        val dir = File("test")
        dir.deleteRecursively()
    }

    @Test
    fun `should get user info`() = testKtorApp {
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
        val token = client.post("/signin"){
            contentType(ContentType.Application.Json)
            setBody(SignInRequest(userForRegistration.phone, userForRegistrationPassword))
        }.headers["Authorization"]!!
        val response = client.get("/user") {
            headers.append("Authorization", token)
        }
        assertThat(response.body<User>())
            .isEqualTo(userForRegistration)
    }
    @Test
    fun `should return 401 if user unauthorized`() = testKtorApp {
        val response = client.get("/user") {
            headers.append("Authorization", "token")
        }
        assertThat(response.status)
            .isEqualTo(HttpStatusCode.Unauthorized)

    }
    @Test
    fun `should update user avatar`() = testKtorApp {
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
        val token = client.post("/signin"){
            contentType(ContentType.Application.Json)
            setBody(SignInRequest(userForRegistration.phone, userForRegistrationPassword))
        }.headers["Authorization"]!!

        val response = client.post("/user/avatar") {
            headers.append("Authorization", token)
            setBody(
                MultiPartFormDataContent(
                    parts = formData {
                        this.append(
                            key = "file",
                            value = ChannelProvider{
                                ClassLoader.getSystemResourceAsStream("avatar/AppLogo.png")!!
                                    .toByteReadChannel()
                            },
                            headers = Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"AppLogo.png\"")
                                append(HttpHeaders.ContentType, "image/png")
                            }
                        )
                    }

                )
            )
        }
        assertThat(response.status)
            .isEqualTo(HttpStatusCode.Accepted)
        val filename = response.body<AvatarUpdateResponse>().user.avatarFilename!!
        val providedFileBytes = ClassLoader.getSystemResourceAsStream("avatar/AppLogo.png")!!.toByteReadChannel().toByteArray()
        val savedFile = File(this.application.getKoin().get<AppConfig>().avatarResourcePath + filename)
        val savedFileBytes = savedFile.readBytes()
        assertThat(savedFileBytes)
            .isEqualTo(providedFileBytes)
    }
}