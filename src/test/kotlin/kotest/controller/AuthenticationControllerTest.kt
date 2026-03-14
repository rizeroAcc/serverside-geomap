package com.mapprjct.kotest.controller

import com.mapprjct.builders.createRegistrationRequest
import com.mapprjct.builders.createTestUser
import com.mapprjct.model.datatype.Password
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.dto.User
import com.mapprjct.model.request.auth.SignInRequest
import com.mapprjct.model.response.auth.RegistrationResponse
import com.mapprjct.service.UserService
import com.mapprjct.testKtorApp
import com.mapprjct.utils.Either
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.server.sessions.SessionStorage
import org.koin.ktor.ext.getKoin
import org.testcontainers.containers.PostgreSQLContainer

class AuthenticationControllerTest : FunSpec() {
    init {
        val postgres = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testdb")
            .withUsername("postgres")
            .withPassword("postgres")
            .withReuse(true)

        install(TestContainerSpecExtension(postgres))

        context("registration"){
            testKtorApp(postgres) {
                val userService = this.application.getKoin().get<UserService>()
                test("should register new user in system"){
                    val user = createTestUser { phone = "89036559989"}
                    val registrationRequest = createRegistrationRequest {
                        forUser(user)
                    }
                    val response = client.post("/register") {
                        setBody(registrationRequest)
                    }
                    response shouldHaveStatus HttpStatusCode.Created
                    response.body<RegistrationResponse>() shouldBe RegistrationResponse(user)
                    userService.getUser(user.phone).getOrNull() shouldBe
                        user.copy(
                            phone = RussiaPhoneNumber(user.phone.normalizeAsRussiaPhone())
                        )

                }
                test("should respond 409 if user already registered"){
                    val user = createTestUser { phone = "89038518685" }
                    val registrationRequest = createRegistrationRequest {
                        forUser(user)
                    }
                    client.post("/register") {
                        setBody(registrationRequest)
                    }
                    client.post("/register") {
                        setBody(registrationRequest)
                    } shouldHaveStatus HttpStatusCode.Conflict
                }
            }
        }
        context("authorization"){
            testKtorApp(postgres) {
                val sessionStorage = this.application.getKoin().get<SessionStorage>()
                val user = createTestUser {  }
                val userPassword = Password("testPassword")
                val registrationRequest = createRegistrationRequest {
                    forUser(user)
                    withPassword(userPassword.value)
                }
                client.post("/register") {
                    setBody(registrationRequest)
                }
                test("should authorize user"){
                    val signInRequest = SignInRequest(
                        phone = user.phone,
                        password = userPassword
                    )
                    val response = client.post("/signin") {
                        setBody(signInRequest)
                    }
                    response shouldHaveStatus HttpStatusCode.OK
                    val responseToken = response.headers["Authorization"]!!
                    shouldNotThrowAny {
                        sessionStorage.read(responseToken)
                    }
                }
                test("should respond 401 if credentials invalid"){
                    val signInRequest = SignInRequest(
                        phone = user.phone,
                        password = Password("userPassword")
                    )
                    client.post("/signin") {
                        setBody(signInRequest)
                    } shouldHaveStatus HttpStatusCode.Unauthorized
                }
            }
        }
        context("logout"){
            testKtorApp(postgres) {
                val sessionStorage = this.application.getKoin().get<SessionStorage>()
                val user = createTestUser {
                    phone = "89036559989"
                    username = "kirill"
                }
                val userPassword = Password("testPassword")
                val registrationRequest = createRegistrationRequest {
                    forUser(user)
                    withPassword(userPassword.value)
                }
                client.post("/register") {
                    setBody(registrationRequest)
                }
                test("should clear user sessions"){
                    val signInRequest = SignInRequest(
                        phone = user.phone,
                        password = userPassword
                    )
                    val token = client.post("/signin") {
                        setBody(signInRequest)
                    }.headers["Authorization"]!!

                    val response = client.post("/logout") {
                        headers.append("Authorization", token)
                    }
                    response shouldHaveStatus HttpStatusCode.Accepted
                    shouldThrow<NoSuchElementException> {
                        sessionStorage.read(token)
                    }
                }
                test("should answer 404 if session invalid"){
                    client.post("/logout") {
                        headers.append("Authorization", "token")
                    } shouldHaveStatus HttpStatusCode.NotFound
                }
                test("should answer 204 if session isn't set"){
                    client.post("/logout") shouldHaveStatus HttpStatusCode.NoContent
                }
            }
        }
    }
}