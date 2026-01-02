package com.mapprjct.service

import com.mapprjct.database.daoimpl.InvitationRepositoryImpl
import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.repositoryImpl.UserRepositoryImpl
import com.mapprjct.database.tables.UserTable
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
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals

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

}