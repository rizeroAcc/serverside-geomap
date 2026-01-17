package com.mapprjct.repository

import com.mapprjct.builders.createCredentials
import com.mapprjct.builders.createTestUser
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.repositoryImpl.UserRepositoryImpl
import com.mapprjct.database.tables.UserTable
import com.mapprjct.model.dto.User
import com.mapprjct.model.dto.UserCredentials
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.eq
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
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
        val user = createTestUser()
        val userPassword = "testPassword"
        suspendTransaction(database) {
            //when
            userRepository.insert(user = user, userPassword)
            //then
            UserTable.selectAll()
                .where{ UserTable.phone eq user.phone }.
                single().let { row->
                    assertThat(row).satisfies(
                        { it[UserTable.phone] == user.phone },
                        { it[UserTable.username] == user.username },
                        { it[UserTable.passwordHash] == userPassword},
                        { it[UserTable.avatar] == user.avatarFilename}
                    )
            }
        }
    }
    @Test
    fun `should throw error unique constraint, if user with same phone already exists`() = runTest {
        //given
        val user = createTestUser()
        val userPassword = "testPassword"
        suspendTransaction(database) {
            //when
            userRepository.insert(user = user, userPassword)
            val result = runCatching {
                userRepository.insert(user = user, userPassword)
            }
            val exception = result.exceptionOrNull()
            //then
            assertThat(exception)
                .isNotNull()
                .isInstanceOf(ExposedSQLException::class.java)
                .extracting { (it as ExposedSQLException).sqlState }
                .isEqualTo("23505") //UNIQUE violation
        }
    }
    @Test
    fun `should throw error, if user have invalid phone`() = runTest {
        //given
        val userWithInvalidPhone = createTestUser {
            phone = "8903655998"
        }
        suspendTransaction(database) {
            //when
            val exception = runCatching {
                userRepository.insert(user = userWithInvalidPhone, "test")
            }.exceptionOrNull()
            assertThat(exception)
                .isNotNull()
                .isInstanceOf(ExposedSQLException::class.java)
                .extracting { (it as ExposedSQLException).sqlState }
                .isEqualTo("23514") //CHECK constraint violation
        }
    }
    @Test
    fun `should throw error, if user have empty username`() = runTest {
        val userWithEmptyUsername = createTestUser {
            username = ""
        }
        suspendTransaction(database) {
            //when
            val exception = runCatching {
                userRepository.insert(user = userWithEmptyUsername, "test")
            }.exceptionOrNull()
            assertThat(exception)
                .isNotNull()
                .isInstanceOf(ExposedSQLException::class.java)
                .extracting { (it as ExposedSQLException).sqlState }
                .isEqualTo("23514") //CHECK constraint violation
        }
    }
    @Test
    fun `should receive existing user`() = runTest {
        //given
        val userToInsert = createTestUser()
        suspendTransaction(database) {
            //when
            userRepository.insert(userToInsert, "test")
            val user = userRepository.getUser(userToInsert.phone)
            //then
            assertThat(user)
                .isNotNull()
                .isEqualTo(userToInsert)
        }
    }
    @Test
    fun `should return null if user does not exist`() = runTest {
        //given : user does not exist
        val unexistedUserPhone = "89036559989"
        suspendTransaction(database) {
            //when
            val result = userRepository.getUser(unexistedUserPhone)
            //then
            assertThat(result)
                .isNull()
        }
    }
    @Test
    fun `should update user fields except phone and password`() = runTest {
        //given
        val user = createTestUser()
        val userInfoForUpdate = createTestUser {
            username = "updatedUserName"
            avatarFilename = "updatedUserAvatarFilename"
        }
        suspendTransaction(database) {
            userRepository.insert(user, "testPassword")
            //when
            val updatedUser = userRepository.updateUser(userInfoForUpdate)
            assertThat(updatedUser)
                .isNotNull()
                .isEqualTo(userInfoForUpdate)
        }
    }

    @Test
    fun `should receive user credentials`() = runTest {
        //given
        val user = createTestUser()
        val userPassword = "testPassword"
        val expectedCredentials = createCredentials {
            forUser(user)
            password = userPassword
        }
        suspendTransaction(database) {
            //when
            userRepository.insert(user, userPassword)
            val receivedCredentials = userRepository.getUserCredentials("89036559989")
            //then
            assertThat(receivedCredentials)
                .isNotNull()
                .isEqualTo(expectedCredentials)
        }
    }
    @Test
    fun `should receive null instead credentials when user doesn't exist`() = runTest {
        //given
        val unexistedUserPhone = "89036559989"
        suspendTransaction(database) {
            //when
            val receivedCredentials = userRepository.getUserCredentials(unexistedUserPhone)
            //then
            assertThat(receivedCredentials)
                .isNull()
        }
    }
    @Test
    fun `should update user password`() = runTest {
        //given
        val password = "testPassword"
        val newPassword = "newPassword"
        val user = createTestUser {
            phone = "89036559989"
            username = "userName"
        }
        suspendTransaction(database) {
            userRepository.insert(user, password)
            //when
            userRepository.updateUserPassword(userPhone = user.phone, password = newPassword)
            val receivedCredentials = userRepository.getUserCredentials(user.phone)
            //then
            assertThat(receivedCredentials)
                .isNotNull()
                .extracting { it!!.password }
                .isEqualTo(newPassword)
        }
    }
}