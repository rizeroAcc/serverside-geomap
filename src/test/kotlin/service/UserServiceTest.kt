package com.mapprjct.service

import com.mapprjct.database.daoimpl.InvitationRepositoryImpl
import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.repositoryImpl.UserRepositoryImpl
import com.mapprjct.database.tables.UserTable
import com.mapprjct.exceptions.BaseAppException
import com.mapprjct.exceptions.user.UserDMLExceptions
import com.mapprjct.exceptions.user.UserValidationException
import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.repository.InvitationRepositoryTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class UserServiceTest {
    companion object {
        @Container
        val postgreSQLContainer = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testDb")
            .withUsername("postgres")
            .withPassword("test")
            .withReuse(true)
    }

    private lateinit var database: Database

    private lateinit var userRepository: UserRepository
    private lateinit var userService: UserService

    @BeforeAll
    fun initialize() {
        database = Database.connect(
            url = postgreSQLContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgreSQLContainer.username,
            password = postgreSQLContainer.password,
        )
        userRepository = UserRepositoryImpl(database)
        userService = UserService(database = database, userRepository = userRepository)
    }

    @BeforeEach
    fun setUp() = runBlocking {

        suspendTransaction(database) {
            SchemaUtils.drop(UserTable)
            SchemaUtils.create(UserTable)
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
    fun `should return null if user doesn't exists`() = runTest {
        //given: user doesn't exists
        //when
        val result = userService.getUser("89036559989")
        //then
        assertNull(result.getOrThrow())
    }

    @Test
    fun `should update user`() = runTest {
        //given
        val credentials = UserCredentials("89036559989", "12345678")
        val username = "test"
        suspendTransaction {
            val createdUser = userService.createUser(credentials,username).getOrThrow()
            val updatedUser = createdUser.copy(username = "new_test", avatarPath = "path")
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
            val createdUser = userService.createUser(credentials,username).getOrThrow()
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
            val createdUser = userService.createUser(credentials,username).getOrThrow()
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
        val username = "test"
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
}