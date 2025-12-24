package com.mapprjct.dao

import com.mapprjct.database.dao.UserDAO
import com.mapprjct.database.daoimpl.UserDAOImpl
import com.mapprjct.database.tables.UserTable
import com.mapprjct.dto.User
import com.mapprjct.dto.UserCredentials
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.SQLException
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue


//TODO Add check for phone length and test for it

@Testcontainers
class UserDAOTest{

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
    private lateinit var userDAO: UserDAO

    @BeforeEach
    fun setUp() {
        database = Database.connect(
            url = databaseContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = databaseContainer.username,
            password = databaseContainer.password
        )
        userDAO = UserDAOImpl(database)
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

        userDAO.insert(user = user, "test")

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
        val dao = userDAO

        dao.insert(user = user, "test")
        val result = runCatching {
            dao.insert(user = user, "test1")
        }

        assertNotNull(result.exceptionOrNull())
        assertTrue(((result.exceptionOrNull() as ExposedSQLException).cause as SQLException).message!!.contains("unique constraint"))
    }

    @Test
    fun `should throw error check constraint, if user phone invalid`() = runTest {
        val user = User(
            phone = "9036559989",
            username = "test",
            avatarPath = null
        )

        val result = runCatching {
            userDAO.insert(user = user, "test")
        }

        assertNotNull(result.exceptionOrNull())
        assertTrue(((result.exceptionOrNull() as ExposedSQLException).cause as SQLException).message!!.contains("check constraint \"phone_valid\""))
    }

    @Test
    fun `should receive existing user`() = runTest {
        val userToInsert = User(phone = "89036559989", username = "test", avatarPath = null)
        userDAO.insert(userToInsert, "test")

        val user = userDAO.getUser("89036559989")

        assertNotNull(user)
        assertEquals(userToInsert, user)
    }

    @Test
    fun `should return null if user does not exist`() = runTest {

        val result = userDAO.getUser("89036559989")

        assertNull(result)
    }

    @Test
    fun `should update user fields except phone and password`() = runTest {
        val user = User(phone = "89036559989", username = "test", avatarPath = null)
        val updatedUserInfo = user.copy(username = "newTest", avatarPath = "testPath")
        userDAO.insert(user, "testPassword")

        val updatedRowsCount = userDAO.updateUser(updatedUserInfo)
        assertEquals(1, updatedRowsCount)

        val updatedUser = userDAO.getUser("89036559989")
        assertEquals(updatedUserInfo,updatedUser)
    }

    @Test
    fun `should receive user credentials`() = runTest {
        val user = User(
            phone = "89036559989",
            username = "testName",
            avatarPath = null
        )

        userDAO.insert(user, "testPassword")
        val receivedCredentials = userDAO.getUserCredentials("89036559989")
        assertNotNull(receivedCredentials)

        val expectedCredentials = UserCredentials(phone = "89036559989", password = "testPassword")
        assertEquals(expectedCredentials, receivedCredentials)
    }

    @Test
    fun `should receive null when user doesn't exist`() = runTest {
        val receivedCredentials = userDAO.getUserCredentials("89036559989")
        assertNull(receivedCredentials)
    }

}