package com.mapprjct.kotest.controller

import com.mapprjct.AppConfig
import com.mapprjct.buildMultipartFromFile
import com.mapprjct.getBean
import com.mapprjct.getTestResourceAsChannel
import com.mapprjct.model.dto.User
import com.mapprjct.model.request.RegistrationRequest
import com.mapprjct.model.request.SignInRequest
import com.mapprjct.model.response.AvatarUpdateResponse
import com.mapprjct.model.response.RegistrationResponse
import com.mapprjct.model.response.error.ErrorResponse
import com.mapprjct.testKtorApp
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.call.body
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.utils.io.toByteArray
import org.assertj.core.api.Assertions.assertThat
import org.testcontainers.containers.PostgreSQLContainer
import java.io.File

class ProfileControllerTest : FunSpec() {
    init {
        val postgres = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testdb")
            .withUsername("postgres")
            .withPassword("postgres")
            .withReuse(true)

        install(TestContainerSpecExtension(postgres))

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

        beforeContainer {
            val testResDir = File("test/api/uploads/avatars/")
            testResDir.listFiles().onEach {
                it.delete()
            }
        }
        context("get profile info"){
            testKtorApp(postgres){
                val (user, token) = createRegisterAndLoginUser()
                test("should get user info"){
                    val response = client.get("/user") {
                        headers.append("Authorization", token)
                    }
                    response shouldHaveStatus HttpStatusCode.OK
                    response.body<User>() shouldBe user
                }
                test("should have status Unauthorized"){
                    val response = client.get("/user") {
                        headers.append("Authorization", "token")
                    }
                    response shouldHaveStatus HttpStatusCode.Unauthorized
                }
            }
        }
        context("update profile avatar"){
            testKtorApp(postgres){
                val (user, token) = createRegisterAndLoginUser()
                test("should update avatar"){
                    val multipartAvatarData = buildMultipartFromFile("avatar/AppLogo.png")
                    val response = client.post("/user/avatar") {
                        headers.append("Authorization", token)
                        setBody(multipartAvatarData)
                    }
                    response shouldHaveStatus HttpStatusCode.Accepted
                    val filename = response.body<AvatarUpdateResponse>().user.avatarFilename!!
                    val providedFileBytes = getTestResourceAsChannel("avatar/AppLogo.png").toByteArray()
                    val savedFile = File(getBean<AppConfig>().avatarResourcePath + filename)
                    savedFile.readBytes() shouldBe providedFileBytes
                }
                test("should respond Unauthorized if token invalid"){
                    val response = client.post("/user/avatar") {
                        headers.append("Authorization", "token")
                    } shouldHaveStatus HttpStatusCode.Unauthorized
                }
                test("should respond Bad request if file is not image"){
                    val avatarData = getTestResourceAsChannel("avatar/AppLogo.png")
                    val multipartAvatarData = buildMultipartFromFile(
                        path ="avatar/AppLogo.png",
                        filename = "AppLogo.html"
                    )
                    val response = client.post("/user/avatar") {
                        headers.append("Authorization", token)
                        setBody(multipartAvatarData)
                    }
                    response shouldHaveStatus HttpStatusCode.BadRequest
                    response.body<ErrorResponse>().message shouldContain "Invalid file format"
                }
                test("should respond ServiceUnavailable if user avatar open in another process"){
                    //todo
                }
            }
        }
    }
}