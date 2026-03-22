package com.mapprjct.kotest.controller

import com.mapprjct.*
import com.mapprjct.builders.createCredentials
import com.mapprjct.exceptions.domain.user.FindUserAvatarError
import com.mapprjct.model.dto.UserDTO
import com.mapprjct.model.request.auth.RegistrationRequest
import com.mapprjct.model.request.auth.SignInRequest
import com.mapprjct.model.request.profile.ChangePasswordRequest
import com.mapprjct.model.request.profile.ChangeUserInfoRequest
import com.mapprjct.model.response.profile.AvatarUpdateResponse
import com.mapprjct.model.response.auth.RegistrationResponse
import com.mapprjct.model.ErrorResponse
import com.mapprjct.model.datatype.Password
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.Username
import com.mapprjct.model.response.profile.UpdateUserInfoResponse
import com.mapprjct.service.UserService
import io.kotest.assertions.ktor.client.shouldHaveContentType
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.utils.io.*
import org.testcontainers.containers.PostgreSQLContainer
import java.io.File
import java.io.RandomAccessFile

class ProfileControllerTest : FunSpec() {
    init {
        val postgres = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testdb")
            .withUsername("postgres")
            .withPassword("postgres")
            .withReuse(true)

        install(TestContainerSpecExtension(postgres))

        suspend fun ApplicationTestBuilder.createRegisterAndLoginUser(
            phone : String = "+79036559989",
            username : String = "admin",
            password : String = "testPassword"
        ) : Pair<UserDTO,String>{
            val registrationRequest = RegistrationRequest(
                RussiaPhoneNumber(phone),
                Username(username),
                Password(password)
            )
            val registrationResponse = client.post("/register") {
                setBody(registrationRequest)
            }.body<RegistrationResponse>()
            val signInResponse = client.post("/signin") {
                setBody(SignInRequest(
                    RussiaPhoneNumber(phone),
                    Password(password)
                ))
            }
            return registrationResponse.userDTO to signInResponse.headers["Authorization"]!!
        }

        beforeContainer {
            val testResDir = File("test/api/uploads/avatars/")
            testResDir.listFiles().onEach {
                it.delete()
            }
        }

        context("update profile avatar"){
            testKtorApp(postgres){
                val appConfig = getBean<AppConfig>()
                val userService = getBean<UserService>()
                val (user, token) = createRegisterAndLoginUser()
                test("should update avatar"){
                    val multipartAvatarData = buildMultipartFromFile("avatar/AppLogo.png")
                    val avatarBytes = getTestResourceAsChannel("avatar/AppLogo.png").toByteArray()
                    val response = client.post("/user/avatar") {
                        headers.append("Authorization", token)
                        setBody(multipartAvatarData)
                    }
                    response shouldHaveStatus HttpStatusCode.Accepted
                    userService.getUserAvatar(user.phone).getOrNull() shouldNotBeNull {
                        this.readBytes() shouldBe avatarBytes
                    }

                }
                test("should respond Unauthorized if token invalid"){
                    val response = client.post("/user/avatar") {
                        headers.append("Authorization", "token")
                    }
                    response shouldHaveStatus HttpStatusCode.Unauthorized
                }
                test("should respond Bad request if file is not image"){
                    val multipartAvatarData = buildMultipartFromFile(
                        path ="avatar/AppLogo.png",
                        filename = "AppLogo.html"
                    )
                    val response = client.post("/user/avatar") {
                        headers.append("Authorization", token)
                        setBody(multipartAvatarData)
                    }
                    response shouldHaveStatus HttpStatusCode.BadRequest
                    response.body<ErrorResponse>().message shouldContain "Invalid avatar format"
                }
                test("should respond Bad request if filename empty"){
                    val multipartAvatarData = buildMultipartFromFile(
                        path = "avatar/AppLogo.png",
                        filename = ""
                    )
                    val response = client.post("/user/avatar") {
                        headers.append("Authorization", token)
                        setBody(multipartAvatarData)
                    }
                    response shouldHaveStatus HttpStatusCode.BadRequest
                    response.body<ErrorResponse>().message shouldContain "Empty file name"
                }
                test("should respond ServiceUnavailable if user avatar open in another process"){
                    val multipartAvatarData = buildMultipartFromFile("avatar/AppLogo.png")
                    val response = client.post("/user/avatar") {
                        headers.append("Authorization", token)
                        setBody(multipartAvatarData)
                    }
                    response shouldHaveStatus HttpStatusCode.Accepted
                    val filename = response.body<AvatarUpdateResponse>().userDTO.avatarFilename!!
                    val avatarFile = File(getBean<AppConfig>().avatarResourcePath + filename)
                    //lock file
                    RandomAccessFile(avatarFile, "rw").use{ randomAccessFile ->
                        randomAccessFile.channel.lock().use {
                            client.post("/user/avatar") {
                                headers.append("Authorization", token)
                                setBody(multipartAvatarData)
                            } shouldHaveStatus HttpStatusCode.ServiceUnavailable
                        }
                    }
                }
            }
        }
        context("get profile avatar"){
            testKtorApp(postgres){
                val (userWithAvatar, tokenAvatar) = createRegisterAndLoginUser(phone = "89036559989")
                val (userWithoutAvatar, tokenWA) = createRegisterAndLoginUser(phone = "89038518685")
                val multipartAvatarData = buildMultipartFromFile("avatar/AppLogo.png")
                client.post("/user/avatar") {
                    headers.append("Authorization", tokenAvatar)
                    setBody(multipartAvatarData)
                }
                test("should get user avatar"){
                    val realAvatarBytes = getTestResourceAsChannel("avatar/AppLogo.png").toByteArray()

                    val response = client.get("/user/avatar") {
                        headers.append("Authorization", tokenAvatar)
                    }

                    response shouldHaveStatus HttpStatusCode.OK
                    response shouldHaveContentType ContentType.Image.PNG
                    response.bodyAsBytes() shouldBe realAvatarBytes
                }
                test("should get avatar bytes by range"){
                    val realAvatarBytes = getTestResourceAsChannel("avatar/AppLogo.png").toByteArray()
                    val fileSize = realAvatarBytes.size.toLong()
                    val start = 1000
                    val end = 1999
                    val rangeLength = (end - start + 1)

                    val partialResponse = client.get("/user/avatar") {
                        headers.append("Authorization", tokenAvatar)
                        header(HttpHeaders.Range, "bytes=$start-$end")
                    }

                    partialResponse shouldHaveStatus HttpStatusCode.PartialContent
                    partialResponse.headers[HttpHeaders.ContentRange] shouldBe "bytes $start-$end/$fileSize"
                    partialResponse.headers[HttpHeaders.ContentLength] shouldBe  rangeLength.toString()
                    partialResponse.bodyAsBytes() shouldBe realAvatarBytes.copyOfRange(start,end + 1)
                }
                test("should respond NoContent if user haven't avatar"){
                    client.get("/user/avatar") {
                        headers.append("Authorization", tokenWA)
                    } shouldHaveStatus HttpStatusCode.NoContent
                }
            }
        }
        context("delete profile avatar"){
            testKtorApp(postgres){
                val userService = getBean<UserService>()


                val (userWithoutAvatar, tokenWA) = createRegisterAndLoginUser(phone = "89038518685")

                test("should delete user avatar"){
                    val (user, userToken) = createRegisterAndLoginUser()
                    val multipartAvatarData = buildMultipartFromFile("avatar/AppLogo.png")
                    client.post("/user/avatar") {
                        headers.append("Authorization", userToken)
                        setBody(multipartAvatarData)
                    }
                    val response = client.delete("/user/avatar"){
                        headers.append("Authorization", userToken)
                    }
                    response shouldHaveStatus HttpStatusCode.OK

                    userService.getUserAvatar(user.phone).getOrNull() shouldBe FindUserAvatarError.UserAvatarNotFound

                }
                test("should respond NotFound if user haven't avatar"){
                    client.delete("/user/avatar") {
                        headers.append("Authorization", tokenWA)
                    } shouldHaveStatus HttpStatusCode.NotFound
                }
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
                    response.body<UserDTO>() shouldBe user
                }
                test("should have status Unauthorized"){
                    val response = client.get("/user") {
                        headers.append("Authorization", "token")
                    }
                    response shouldHaveStatus HttpStatusCode.Unauthorized
                }
            }
        }
        context("change password"){
            testKtorApp(postgres){
                val userService = getBean<UserService>()
                test("should change user password"){
                    val (oldUserPassword, newUserPassword) = "oldUserPassword" to "newUserPassword"
                    val (user, userToken) = createRegisterAndLoginUser(phone = "89036559989", password = oldUserPassword)
                    val request = ChangePasswordRequest(
                        oldPassword = oldUserPassword,
                        newPassword = newUserPassword
                    )
                    val response = client.post("/user/changePassword") {
                        headers.append("Authorization", userToken)
                        setBody(request)
                    }
                    response shouldHaveStatus HttpStatusCode.Accepted
                    //check passwor updated
                    userService.validateCredentials(createCredentials {
                        forUser(user)
                        password = newUserPassword
                    })
                }
                test("should recreate user session after password change"){
                    val (oldUserPassword, newUserPassword) = "oldUserPassword" to "newUserPassword"
                    val (user, userToken) = createRegisterAndLoginUser(phone = "89038518685", password = oldUserPassword)
                    val request = ChangePasswordRequest(
                        oldPassword = oldUserPassword,
                        newPassword = newUserPassword
                    )
                    val response = client.post("/user/changePassword") {
                        headers.append("Authorization", userToken)
                        setBody(request)
                    }
                    val newToken = response.headers["Authorization"] shouldNotBe null
                    //check old token invalid
                    client.get("/user"){
                        headers.append("Authorization", userToken)
                    } shouldHaveStatus HttpStatusCode.Unauthorized
                    //check new token valid
                    newToken shouldNotBe userToken
                    client.get("/user"){
                        headers.append("Authorization", newToken!!)
                    } shouldHaveStatus HttpStatusCode.OK
                }
                test("should respond bad request if old password does not match"){
                    val oldUserPassword = "oldUserPassword"
                    val wrongPass = "wrongUserPassword"
                    val (user, userToken) = createRegisterAndLoginUser(phone = "89203415678",password = oldUserPassword)
                    val changePasswordRequest = ChangePasswordRequest(
                        oldPassword = wrongPass,
                        newPassword = wrongPass
                    )
                    client.post("/user/changePassword") {
                        headers.append("Authorization", userToken)
                        setBody(changePasswordRequest)
                    } shouldHaveStatus HttpStatusCode.Forbidden
                }
            }
        }
        context("update profile info"){
            testKtorApp(postgres){
                val userService = getBean<UserService>()
                val (user,token) = createRegisterAndLoginUser()
                test("should update user profile"){
                    val changeUserInfoRequest = ChangeUserInfoRequest(
                        userDTO = user.copy(username = Username("updated user name")),
                    )
                    val response = client.patch("/user"){
                        headers.append("Authorization", token)
                        setBody(changeUserInfoRequest)
                    }
                    response shouldHaveStatus HttpStatusCode.Accepted
                    val newUserInfo = response.body<UpdateUserInfoResponse>().userDTO
                    userService.getUser(user.phone).leftOrNull() shouldBe newUserInfo
                }
                test("should respond BadRequest if try to change phone"){
                    val changeUserInfoRequest = ChangeUserInfoRequest(
                        userDTO = user.copy(phone = RussiaPhoneNumber("89038518685"))
                    )
                    client.patch("/user"){
                        headers.append("Authorization", token)
                        setBody(changeUserInfoRequest)
                    } shouldHaveStatus HttpStatusCode.BadRequest
                }
            }
        }
    }
}