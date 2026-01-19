package com.mapprjct.junit.controller

import com.mapprjct.AppConfig
import com.mapprjct.ApplicationStartMode
import com.mapprjct.database.tables.InviteCodeTable
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.database.tables.SessionTable
import com.mapprjct.database.tables.UserTable
import com.mapprjct.getBean
import com.mapprjct.getTestResourceAsChannel
import com.mapprjct.model.dto.User
import com.mapprjct.model.request.RegistrationRequest
import com.mapprjct.model.request.SignInRequest
import com.mapprjct.model.response.AvatarUpdateResponse
import com.mapprjct.model.response.RegistrationResponse
import com.mapprjct.module
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
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
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
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
            defaultRequest {
                contentType(ContentType.Application.Json)
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
        startApplication()
        block()
    }

    suspend fun ApplicationTestBuilder.createRegisterAndLoginUser(
        phone : String = "89036559989",
        username : String = "admin",
        password : String = "testPassword"
    ) : Pair<User,String>{
        val registrationRequest = RegistrationRequest(phone, username, password)
        val registrationResponse = client.post("/register") {
            setBody(registrationRequest)
        }.body<RegistrationResponse>()
        val signInResponse = client.post("/signin") {
            setBody(SignInRequest(phone, password))
        }
        return registrationResponse.user to signInResponse.headers["Authorization"]!!
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
    fun setUp() : Unit = runBlocking {
        suspendTransaction {
            SchemaUtils.drop(
                UserTable,
                SessionTable,
                ProjectTable,
                ProjectUsersTable,
                InviteCodeTable
            )
        }
        val dir = File("test")
        dir.deleteRecursively()
    }

    @Nested
    inner class GetProfileInfo{
        @Test
        fun `should get user info`() = testKtorApp {
            val (user, token) = createRegisterAndLoginUser()
            val response = client.get("/user") {
                headers.append("Authorization", token)
            }
            assertThat(response.body<User>())
                .isEqualTo(user)
        }
        @Test
        fun `should return 401 if user unauthorized`() = testKtorApp {
            val response = client.get("/user") {
                headers.append("Authorization", "token")
            }
            assertThat(response.status)
                .isEqualTo(HttpStatusCode.Unauthorized)
        }
    }

    @Nested
    inner class UpdateProfileAvatar{
        @Test
        fun `should update user avatar`() = testKtorApp {
            val (user, token) = createRegisterAndLoginUser()
            val avatarData = getTestResourceAsChannel("avatar/AppLogo.png")
            val response = client.post("/user/avatar") {
                headers.append("Authorization", token)
                setBody(
                    MultiPartFormDataContent(
                        parts = formData {
                            this.append(
                                key = "file",
                                value = ChannelProvider{ avatarData },
                                headers = Headers.build {
                                    append(HttpHeaders.ContentDisposition, "filename=\"AppLogo.png\"")
                                    append(HttpHeaders.ContentType, "image/png")
                                }
                            )
                        }
                    )
                )
            }
            //check avatar updated
            assertThat(response.status)
                .isEqualTo(HttpStatusCode.Accepted)
            val filename = response.body<AvatarUpdateResponse>().user.avatarFilename!!
            val providedFileBytes = getTestResourceAsChannel("avatar/AppLogo.png").toByteArray()
            val savedFile = File(getBean<AppConfig>().avatarResourcePath + filename)
            val savedFileBytes = savedFile.readBytes()
            assertThat(savedFileBytes)
                .isEqualTo(providedFileBytes)
        }
    }

}