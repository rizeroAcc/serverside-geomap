package com.mapprjct.service

import com.mapprjct.AppConfig
import com.mapprjct.database.storage.AvatarStorage
import com.mapprjct.database.tables.UserTable
import com.mapprjct.di.repositoryModule
import com.mapprjct.di.serviceModule
import com.mapprjct.di.storageModule
import com.mapprjct.exceptions.user.UserDMLExceptions
import com.mapprjct.exceptions.user.UserValidationException
import com.mapprjct.model.dto.UserCredentials
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.junit.jupiter.api.*
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class UserServiceTest : KoinTest {
    companion object {
        @Container
        val postgreSQLContainer: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testDb")
            .withUsername("postgres")
            .withPassword("test")
            .withReuse(true)
    }

    private lateinit var database: Database
    private val userService: UserService by inject()
    private val avatarStorage: AvatarStorage by inject()
    @BeforeAll
    fun initialize() {
        database = Database.connect(
            url = postgreSQLContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgreSQLContainer.username,
            password = postgreSQLContainer.password,
        )
        startKoin {
            modules(
                module {
                    single { database }
                    single { AppConfig.Test }
                },
                storageModule,
                repositoryModule,
                serviceModule
            )
        }
    }

    @AfterAll
    fun shutdown() {
        stopKoin()
    }

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        suspendTransaction(database) {
            SchemaUtils.drop(UserTable)
            SchemaUtils.create(UserTable)
        }
        val testResDir = File("test/api/uploads/avatars/")
        testResDir.listFiles().onEach {
            it.delete()
        }
    }

    @Test
    fun `should create user with valid info`() = runTest {
        //given
        val credentials = UserCredentials("89036559989", "12345678")
        val username = "myUser"
        suspendTransaction {
            //when
            userService.createUser(
                userCredentials = credentials,
                username = username
            )
            //then
            UserTable.selectAll().where { UserTable.phone eq credentials.phone }.single().let {
                assertEquals(expected = credentials.phone, it[UserTable.phone])
                assertEquals(expected = credentials.password, it[UserTable.passwordHash])
                assertEquals(expected = username, it[UserTable.username])
            }
        }
    }

    @Test
    fun `should return validation error if new user phone incorrect`() = runTest {
        //given
        val credentials = UserCredentials("890365599", "12345678")
        suspendTransaction {
            //when
            val result = userService.createUser(
                userCredentials = credentials,
                username = "test"
            )
            val exception = result.exceptionOrNull()
            //then
            assertNotNull(exception)
            assertTrue{ exception is UserValidationException.InvalidPhoneFormat  }
        }
    }

    @Test
    fun `should return validation error if new user password length less 8 symbols`() = runTest {
        //given
        val credentials = UserCredentials("89036559989", "1234567")
        suspendTransaction {
            //when
            val result = userService.createUser(
                userCredentials = credentials,
                username = "test"
            )
            val exception = result.exceptionOrNull()
            //then
            assertNotNull(exception)
            assertTrue{ exception is UserValidationException.InvalidPasswordLength  }
        }
    }

    @Test
    fun `should return validation error if new user username empty`() = runTest {
        //given
        val credentials = UserCredentials("89036559989", "12345678")
        suspendTransaction {
            //when
            val result = userService.createUser(
                userCredentials = credentials,
                username = "   "
            )
            val exception = result.exceptionOrNull()
            //then
            assertNotNull(exception)
            assertTrue{ exception is UserValidationException.InvalidUsername  }
        }
    }

    @Test
    fun `should return true if credentials are valid`() = runTest {
        //given
        val credentials = UserCredentials("89036559989", "12345678")
        val username = "test"
        suspendTransaction {
            userService.createUser(credentials,username)
            //when
            val result = userService.validateCredentials(credentials)
            //then
            assertTrue { result.getOrThrow() }
        }
    }

    @Test
    fun `should return false if credentials are invalid`() = runTest {
        //given
        val credentials = UserCredentials("89036559989", "12345678")
        val username = "test"
        val invalidCredentials = UserCredentials("89036559989", "87654321")
        suspendTransaction {
            userService.createUser(credentials,username)
            //when
            val result = userService.validateCredentials(invalidCredentials)
            //then
            assertFalse { result.getOrThrow() }
        }
    }

    @Test
    fun `should return existing user`() = runTest {
        //given
        val credentials = UserCredentials("89036559989", "12345678")
        val username = "test"
        suspendTransaction {
            val createdUser = userService.createUser(credentials,username).getOrThrow()
            //when
            val result = userService.getUser(credentials.phone).getOrThrow()
            //then
            assertEquals(createdUser, result)
        }
    }

    @Test
    fun `should return UserNotFoundException user doesn't exists`() = runTest {
        //given: user doesn't exist
        //when
        val result = userService.getUser("89036559989")
        //then
        assertThat(result.exceptionOrNull())
            .isNotNull()
            .isInstanceOf(UserDMLExceptions.UserNotFoundException::class.java)
    }

    @Test
    fun `should update user`() = runTest {
        //given
        val credentials = UserCredentials("89036559989", "12345678")
        val username = "test"
        suspendTransaction {
            val createdUser = userService.createUser(credentials,username).getOrThrow()
            val updatedUser = createdUser.copy(username = "new_test", avatarFilename = "path")
            //when
            val result = userService.updateUser(updatedUser).getOrThrow()
            //then
            assertEquals(updatedUser, result)
        }
    }

    @Test
    fun `should update user password`() = runTest {
        //given
        val credentials = UserCredentials("89036559989", "12345678")
        val newCredentials = credentials.copy(password = "new_password")
        val username = "test"
        suspendTransaction {
            userService.createUser(credentials,username).getOrThrow()
            //when
            userService.updateUserPassword(
                oldCredentials = credentials,
                newUserPassword = newCredentials.password,
            )
            //then
            assertTrue(
                userService.validateCredentials(newCredentials).getOrThrow()
            )
        }
    }

    @Test
    fun `should return IllegalArgumentException if old password incorrect`() = runTest {
        //given
        val credentials = UserCredentials("89036559989", "12345678")
        val newCredentials = credentials.copy(password = "new_password")
        val username = "test"
        suspendTransaction {
            userService.createUser(credentials,username).getOrThrow()
            //when
            val result = userService.updateUserPassword(
                oldCredentials = newCredentials,
                newUserPassword = newCredentials.password,
            )
            //then
            assertTrue { result.exceptionOrNull()!! is IllegalArgumentException }
        }
    }

    @Test
    fun `should return UserNotFoundException if user not found`() = runTest {
        //given
        val credentials = UserCredentials("89036559989", "12345678")
        val newCredentials = credentials.copy(password = "new_password")
        suspendTransaction {
            //when
            val result = userService.updateUserPassword(
                oldCredentials = newCredentials,
                newUserPassword = newCredentials.password,
            )
            //then
            assertTrue { result.exceptionOrNull()!! is UserDMLExceptions.UserNotFoundException }
        }
    }

    @Test
    fun `should update user avatar`() = runTest {
        val user = userService.createUser(UserCredentials("89036559989", "12345678"), "test").getOrThrow()
        val testAvatarData = ClassLoader.getSystemResourceAsStream("avatar/AppLogo.png")!!.toByteReadChannel()

        val updatedUser = userService.updateUserAvatar(
            user = user,
            fileName = "AppLogo.png",
            fileDataChannelProvider = { testAvatarData },
        )
        assertThat(updatedUser.getOrNull())
            .isNotNull()

        val providedFileBytes = ClassLoader.getSystemResourceAsStream("avatar/AppLogo.png")!!.toByteReadChannel().toByteArray()
        val savedAvatarFileBytes = File(avatarStorage.getUploadDirectory(),updatedUser.getOrNull()!!.avatarFilename!!).readBytes()
        assertThat(savedAvatarFileBytes)
            .isEqualTo(providedFileBytes)
    }

    @Test
    fun `should return IllegalArgumentException if file is not image`() = runTest {
        val user = userService.createUser(
            userCredentials = UserCredentials("89036559989", "12345678"),
            username = "test"
        ).getOrThrow()
        val result = userService.updateUserAvatar(
            user = user,
            fileName = "AppLogo.html",
            fileDataChannelProvider = { ByteReadChannel(ByteArray(5)) },
        )
        assertThat(result.exceptionOrNull())
            .isNotNull()
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `should get user avatar`() = runTest {
        val user = userService.createUser(UserCredentials("89036559989", "12345678"), "test").getOrThrow()
        val testAvatarData = ClassLoader.getSystemResourceAsStream("avatar/AppLogo.png")!!.toByteReadChannel()
        val updatedUser = userService.updateUserAvatar(
            user = user,
            fileName = "AppLogo.png",
            fileDataChannelProvider = { testAvatarData },
        ).getOrThrow()
        val avatarFile = userService.getUserAvatar(user.phone).getOrThrow()
        assertThat(avatarFile)
            .satisfies(
                { it.exists() },
                { it.name.equals(updatedUser.avatarFilename) },
                { it.canRead() }
            )
    }

    @Test
    fun `should return UserAvatarNotFound if user hasn't avatar`() = runTest {
        val user = userService.createUser(UserCredentials("89036559989", "12345678"), "test").getOrThrow()
        val result = userService.getUserAvatar(user.phone)
        assertThat(result.exceptionOrNull())
            .isNotNull()
            .isInstanceOf(UserDMLExceptions.UserAvatarNotFoundException::class.java)
    }
}