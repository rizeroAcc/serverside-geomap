package com.mapprjct.dao

import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.daoimpl.UserRepositoryImpl
import com.mapprjct.database.tables.UserTable
import com.mapprjct.model.dto.User
import com.mapprjct.model.dto.UserCredentials
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
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
        val databaseContainer = PostgreSQLContainer("postgres:latest")
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

    @AfterEach
    fun tearDown() {
        transaction(database) {
            SchemaUtils.drop(UserTable)
        }
    }

    @Test
    fun `should insert user`() = runTest {
        val user = User(
            phone = "89036559989",
            username = "test",
            avatarPath = null
        )

        userRepository.insert(user = user, "test")

        transaction(database) {
            UserTable.selectAll().single().let {
                assertEquals(user.username, it[UserTable.username])
                assertEquals(user.phone, it[UserTable.phone])
                assertEquals(user.avatarPath, it[UserTable.avatar])
                assertEquals("test", it[UserTable.passwordHash])
            }
        }
    }

    @Test
    fun `should throw error unique constraint, if user with same phone already exists`() = runTest {
        val user = User(
            phone = "+79036559989",
            username = "test",
            avatarPath = null
        )
        val dao = userRepository

        dao.insert(user = user, "test")
        val result = runCatching {
            dao.insert(user = user, "test1")
        }

        assertNotNull(result.exceptionOrNull())
        assertTrue(((result.exceptionOrNull() as ExposedSQLException).cause as SQLException).message!!.contains("unique constraint"))
    }

    @Test
    fun `should throw error, if user info invalid`() = runTest {
        val userWithInvalidPhone = User(
            phone = "9036559989",
            username = "test",
            avatarPath = null
        )
        val userWithEmptyUsername = User(
            phone = "+79036559989",
            username = "",
            avatarPath = null
        )

        var result = runCatching {
            userRepository.insert(user = userWithInvalidPhone, "test")
        }
        assertNotNull(result.exceptionOrNull())

        result = runCatching {
            userRepository.insert(userWithEmptyUsername, "test")
        }
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `should receive existing user`() = runTest {
        val userToInsert = User(phone = "89036559989", username = "test", avatarPath = null)
        userRepository.insert(userToInsert, "test")

        val user = userRepository.getUser("89036559989")

        assertNotNull(user)
        assertEquals(userToInsert, user)
    }

    @Test
    fun `should return null if user does not exist`() = runTest {

        val result = userRepository.getUser("89036559989")

        assertNull(result)
    }

    @Test
    fun `should update user fields except phone and password`() = runTest {
        val user = User(phone = "89036559989", username = "test", avatarPath = null)
        val updatedUserInfo = user.copy(username = "newTest", avatarPath = "testPath")
        userRepository.insert(user, "testPassword")

        val updatedRowsCount = userRepository.updateUser(updatedUserInfo)
        assertEquals(1, updatedRowsCount)

        val updatedUser = userRepository.getUser("89036559989")
        assertEquals(updatedUserInfo,updatedUser)
    }

    @Test
    fun `should receive user credentials`() = runTest {
        val user = User(
            phone = "89036559989",
            username = "testName",
            avatarPath = null
        )

        userRepository.insert(user, "testPassword")
        val receivedCredentials = userRepository.getUserCredentials("89036559989")
        assertNotNull(receivedCredentials)

        val expectedCredentials = UserCredentials(phone = "89036559989", password = "testPassword")
        assertEquals(expectedCredentials, receivedCredentials)
    }

    @Test
    fun `should receive null when user doesn't exist`() = runTest {
        val receivedCredentials = userRepository.getUserCredentials("89036559989")
        assertNull(receivedCredentials)
    }

    @Test
    fun `should update user password`() = runTest {
        val password = "testPassword"
        val phone = "89036559989"
        val username = "testName"
        val user = User(
            phone = phone,
            username = username,
            avatarPath = null
        )
        val oldCredentials = UserCredentials(phone = phone, password = password)
        userRepository.insert(user, password)

        var receivedCredentials = userRepository.getUserCredentials("89036559989")
        assertEquals(oldCredentials, receivedCredentials)

        val newPassword = "newPassword"
        userRepository.updateUserPassword(userPhone = phone, password = newPassword)
        receivedCredentials = userRepository.getUserCredentials("89036559989")
        assertEquals(newPassword, receivedCredentials!!.password)

    }
}