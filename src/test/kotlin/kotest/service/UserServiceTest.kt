package com.mapprjct.kotest.service

import com.mapprjct.AppConfig
import com.mapprjct.builders.createTestUser
import com.mapprjct.database.storage.AvatarStorage
import com.mapprjct.database.tables.UserTable
import com.mapprjct.di.repositoryModule
import com.mapprjct.di.serviceModule
import com.mapprjct.di.storageModule
import com.mapprjct.exceptions.user.UserDMLExceptions
import com.mapprjct.exceptions.user.UserValidationException
import com.mapprjct.model.dto.User
import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.service.UserService
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.koin.KoinExtension
import io.kotest.koin.KoinLifecycleMode
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotBe
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import io.ktor.utils.io.toByteArray
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.testcontainers.containers.PostgreSQLContainer
import java.io.File
import kotlin.test.assertTrue

class UserServiceTest : KoinTest, FunSpec(){
    init {
        val postgres = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testdb")
            .withUsername("postgres")
            .withPassword("postgres")
            .withReuse(true)

        val database by lazy {
            Database.connect(
                url = postgres.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = postgres.username,
                password = postgres.password
            )
        }

        install(TestContainerSpecExtension(postgres))
        extension(
            KoinExtension(
                modules = listOf(
                    module {
                        single { database }
                        single { AppConfig.Test }
                    },
                    storageModule,
                    repositoryModule,
                    serviceModule
                ),
                mode = KoinLifecycleMode.Root
            )
        )
        // Подключаемся один раз к контейнеру

        val userService:UserService by inject()
        val avatarStorage:AvatarStorage by inject()
        beforeSpec {
            suspendTransaction(database) {
                SchemaUtils.create(UserTable)
            }
        }
        beforeTest {
            suspendTransaction(database) {
                UserTable.deleteAll()
            }
            val testResDir = File("test/api/uploads/avatars/")
            testResDir.listFiles().onEach {
                it.delete()
            }
        }
        context("create user") {
            test("should create user with valid info"){
                //given
                val credentials = UserCredentials("89036559989", "12345678")
                val username = "myUser"
                suspendTransaction {
                    //when
                    userService.createUser(userCredentials = credentials, username = username)
                    //then
                    UserTable.selectAll()
                        .where { UserTable.phone eq credentials.phone }
                        .single().asClue {
                        it[UserTable.phone] shouldBe credentials.phone
                        it[UserTable.passwordHash] shouldBe credentials.password
                        it[UserTable.username] shouldBe username
                    }
                }
            }
            test("should return validation error if new user phone incorrect"){
                //given
                val credentials = UserCredentials("890365599", "12345678")
                suspendTransaction {
                    shouldThrow<UserValidationException.InvalidPhoneFormat> {
                        userService.createUser(userCredentials = credentials, username = "test")
                            .getOrThrow()
                    }
                }
            }
            test("should return validation error if new user password length less 8 symbols"){
                //given
                val credentials = UserCredentials("89036559989", "1234567")
                suspendTransaction {
                    shouldThrow<UserValidationException.InvalidPasswordLength> {
                        userService.createUser(userCredentials = credentials, username = "test")
                            .getOrThrow()
                    }
                }
            }
            test("should return validation error if new user username empty"){
                val credentials = UserCredentials("89036559989", "12345678")
                suspendTransaction {
                    shouldThrow<UserValidationException.InvalidUsername> {
                        userService.createUser(userCredentials = credentials, username = "    ")
                            .getOrThrow()
                    }
                }
            }
        }
        context("credentials validation"){
            test("should return true if credentials are valid"){
                val user = createTestUser {  }
                val userCredentials = UserCredentials(user.phone, "12345678")
                userService.createUser(userCredentials = userCredentials, username = user.username)
                val validCredentials = userCredentials.copy()
                userService.validateCredentials(validCredentials).getOrThrow() shouldBe true
            }
            test("should return false if credentials are invalid"){
                val user = createTestUser {  }
                val userCredentials = UserCredentials(user.phone, "12345678")
                userService.createUser(userCredentials = userCredentials, username = user.username)
                val invalidCredentials = userCredentials.copy(password = "87654321")
                userService.validateCredentials(invalidCredentials).getOrThrow() shouldBe false
            }
        }
        context("get user"){
            test("should return existing user"){
                val user = createTestUser {  }
                val userCredentials = UserCredentials(user.phone, "12345678")
                userService.createUser(userCredentials = userCredentials, username = user.username)
                userService.getUser(user.phone).getOrThrow() shouldBe user
            }
            test("should return UserNotFoundException user doesn't exists"){
                val unregisteredUserPhone = "89038518685"
                shouldThrow<UserDMLExceptions.UserNotFoundException> {
                    userService.getUser(unregisteredUserPhone).getOrThrow()
                }
            }
        }
        context("update user"){
            test("should update user"){
                val user = createTestUser {  }
                val userCredentials = UserCredentials(user.phone, "12345678")
                userService.createUser(userCredentials = userCredentials, username = user.username)

                val updatedUser = user.copy(username = "new_test", avatarFilename = "path")
                userService.updateUser(updatedUser).getOrThrow() shouldBe updatedUser
            }
            test("should update user password"){val user = createTestUser {  }
                val userCredentials = UserCredentials(user.phone, "12345678")
                userService.createUser(userCredentials = userCredentials, username = user.username)
                val newCredentials = userCredentials.copy(password = "new_password")
                userService.updateUserPassword(
                    oldCredentials = userCredentials,
                    newUserPassword = newCredentials.password,
                )
                userService.validateCredentials(newCredentials).getOrThrow() shouldBe true
            }
            test("should return IllegalArgumentException if old password incorrect"){val user = createTestUser {  }
                val userCredentials = UserCredentials(user.phone, "12345678")
                userService.createUser(userCredentials = userCredentials, username = user.username)
                val newCredentials = userCredentials.copy(password = "new_password")
                shouldThrow<IllegalArgumentException>{
                    userService.updateUserPassword(
                        oldCredentials = newCredentials,
                        newUserPassword = newCredentials.password
                    ).getOrThrow()
                }
            }
            test("should return UserNotFoundException if user not found"){
                val credentials = UserCredentials("89036559989", "12345678")
                val newCredentials = credentials.copy(password = "new_password")
                shouldThrow<UserDMLExceptions.UserNotFoundException> {
                    userService.updateUserPassword(
                        oldCredentials = newCredentials,
                        newUserPassword = newCredentials.password,
                    ).getOrThrow()
                }
            }
        }
        context("update avatar"){
            test("should update user avatar"){
                val user = userService.createUser(UserCredentials("89036559989", "12345678"), "test").getOrThrow()
                val testAvatarData = ClassLoader.getSystemResourceAsStream("avatar/AppLogo.png")!!.toByteReadChannel()

                val updatedUser = userService.updateUserAvatar(
                    user = user,
                    fileName = "AppLogo.png",
                    fileDataChannelProvider = { testAvatarData },
                ) shouldNotBe null

                val providedFileBytes = ClassLoader.getSystemResourceAsStream("avatar/AppLogo.png")!!.toByteReadChannel().toByteArray()
                val savedAvatarFileBytes = File(
                    avatarStorage.getUploadDirectory(),
                    updatedUser.getOrNull()!!.avatarFilename!!
                ).readBytes() shouldBe providedFileBytes
            }
            test("should return IllegalArgumentException if file is not image"){
                val user = userService.createUser(
                    userCredentials = UserCredentials("89036559989", "12345678"),
                    username = "test"
                ).getOrThrow()
                shouldThrow<IllegalArgumentException>{
                    userService.updateUserAvatar(
                        user = user,
                        fileName = "AppLogo.html",
                        fileDataChannelProvider = { ByteReadChannel(ByteArray(5)) },
                    ).getOrThrow()
                }
            }
        }
        context("get avatar"){
            test("should get user avatar"){
                val user = userService.createUser(UserCredentials("89036559989", "12345678"), "test").getOrThrow()
                val testAvatarData = ClassLoader.getSystemResourceAsStream("avatar/AppLogo.png")!!.toByteReadChannel()
                val updatedUser = userService.updateUserAvatar(
                    user = user,
                    fileName = "AppLogo.png",
                    fileDataChannelProvider = { testAvatarData },
                ).getOrThrow()
                userService.getUserAvatar(user.phone).getOrThrow().asClue {
                    it.exists() shouldBe true
                    it.name shouldBe updatedUser.avatarFilename
                    it.canRead() shouldBe true
                }
            }
            test("should return UserAvatarNotFound if user hasn't avatar"){
                val user = userService.createUser(UserCredentials("89036559989", "12345678"), "test").getOrThrow()
                shouldThrow<UserDMLExceptions.UserAvatarNotFoundException> {
                    userService.getUserAvatar(user.phone).getOrThrow()
                }
            }
        }
    }
}