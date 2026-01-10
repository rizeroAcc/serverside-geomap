package com.mapprjct.repository

import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.repositoryImpl.UserRepositoryImpl
import com.mapprjct.database.tables.UserTable
import com.mapprjct.model.dto.User
import com.mapprjct.model.dto.UserCredentials
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertNotNull
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.SQLException
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepositoryTest{

    companion object {
        @Container
        @JvmStatic
        val databaseContainer: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testdb")
            .withUsername("postgres")
            .withPassword("postgres")
            .withReuse(true)
    }

    private lateinit var database: Database
    private lateinit var userRepository: UserRepository

    @BeforeAll
    fun initialize(){
        database = Database.connect(
            url = databaseContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = databaseContainer.username,
            password = databaseContainer.password
        )
        userRepository = UserRepositoryImpl(database)
    }

    @BeforeEach
    fun setUp() {
        transaction(database) {
            SchemaUtils.drop(UserTable)
            SchemaUtils.create(UserTable)
        }
    }

    @Test
    fun `should insert user`() = runTest {
        //given
        val user = User(
            phone = "89036559989",
            username = "test",
            avatarFilename = null
        )

        suspendTransaction(database) {
            //when
            userRepository.insert(user = user, "test")
            //then
            UserTable.selectAll().single().let {
                assertEquals(user.username, it[UserTable.username])
                assertEquals(user.phone, it[UserTable.phone])
                assertEquals(user.avatarFilename, it[UserTable.avatar])
                assertEquals("test", it[UserTable.passwordHash])
            }
        }
    }

    @Test
    fun `should throw error unique constraint, if user with same phone already exists`() = runTest {
        //given
        val user = User(
            phone = "+79036559989",
            username = "test",
            avatarFilename = null
        )

        suspendTransaction(database) {
            //when
            userRepository.insert(user = user, "test")
            val result = runCatching {
                userRepository.insert(user = user, "test1")
            }
            //then
            assertNotNull(result.exceptionOrNull())
            assertTrue(((result.exceptionOrNull() as ExposedSQLException).cause as SQLException).message!!.contains("unique constraint"))
        }
    }

    @Test
    fun `should throw error, if user info invalid`() = runTest {
        //given
        val userWithInvalidPhone = User(
            phone = "9036559989",
            username = "test",
            avatarFilename = null
        )
        val userWithEmptyUsername = User(
            phone = "+79036559989",
            username = "",
            avatarFilename = null
        )

        suspendTransaction(database) {
            //when
            var result = runCatching {
                userRepository.insert(user = userWithInvalidPhone, "test")
            }
            //then
            assertNotNull(result.exceptionOrNull())

            //when
            result = runCatching {
                userRepository.insert(userWithEmptyUsername, "test")
            }
            //then
            assertNotNull(result.exceptionOrNull())
        }
    }

    @Test
    fun `should receive existing user`() = runTest {
        //given
        val userToInsert = User(phone = "89036559989", username = "test", avatarFilename = null)
        suspendTransaction(database) {
            //when
            userRepository.insert(userToInsert, "test")
            val user = userRepository.getUser("89036559989")
            //then
            assertNotNull(user)
            assertEquals(userToInsert, user)
        }
    }

    @Test
    fun `should return null if user does not exist`() = runTest {
        //given : user does not exist
        suspendTransaction(database) {
            //when
            val result = userRepository.getUser("89036559989")
            //then
            assertNull(result)
        }
    }

    @Test
    fun `should update user fields except phone and password`() = runTest {
        //given
        val user = User(phone = "89036559989", username = "test", avatarFilename = null)
        val updatedUserInfo = user.copy(username = "newTest", avatarFilename = "testPath")
        suspendTransaction(database) {
            userRepository.insert(user, "testPassword")
            //when
            val updatedUser = userRepository.updateUser(updatedUserInfo)
            assertNotNull(updatedUser)
            assertEquals(updatedUserInfo,updatedUser)
        }
    }

    @Test
    fun `should receive user credentials`() = runTest {
        //given
        val user = User(
            phone = "89036559989",
            username = "testName",
            avatarFilename = null
        )
        val expectedCredentials = UserCredentials(phone = "89036559989", password = "testPassword")
        suspendTransaction(database) {
            //when
            userRepository.insert(user, "testPassword")
            val receivedCredentials = userRepository.getUserCredentials("89036559989")
            //then
            assertNotNull(receivedCredentials)
            assertEquals(expectedCredentials, receivedCredentials)
        }
    }

    @Test
    fun `should receive null when user doesn't exist`() = runTest {
        //given : user doesn't exist`
        suspendTransaction(database) {
            //when
            val receivedCredentials = userRepository.getUserCredentials("89036559989")
            //then
            assertNull(receivedCredentials)
        }
    }

    @Test
    fun `should update user password`() = runTest {
        //given
        val password = "testPassword"
        val newPassword = "newPassword"
        val phone = "89036559989"
        val username = "testName"
        val user = User(
            phone = phone,
            username = username,
            avatarFilename = null
        )
        val oldCredentials = UserCredentials(phone = phone, password = password)
        suspendTransaction(database) {
            userRepository.insert(user, password)
            var receivedCredentials = userRepository.getUserCredentials("89036559989")
            assertEquals(oldCredentials, receivedCredentials)
            //when
            userRepository.updateUserPassword(userPhone = phone, password = newPassword)
            receivedCredentials = userRepository.getUserCredentials("89036559989")
            //then
            assertEquals(newPassword, receivedCredentials!!.password)
        }
    }
}