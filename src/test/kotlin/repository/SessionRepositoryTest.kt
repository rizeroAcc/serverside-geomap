package com.mapprjct.dao

import com.mapprjct.database.dao.SessionRepository
import com.mapprjct.database.daoimpl.SessionRepositoryImpl
import com.mapprjct.database.tables.SessionTable
import com.mapprjct.model.APISession
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertNull
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionRepositoryTest(){
    companion object {
        @Container
        val postgreSQLContainer = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testDb")
            .withUsername("postgres")
            .withPassword("test")
            .withReuse(true)
    }

    private lateinit var database: Database
    private lateinit var sessionRepository: SessionRepository

    @BeforeAll
    fun initialize(){
        database = Database.connect(
            url = postgreSQLContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = "postgres",
            password = "test",
        )
        sessionRepository = SessionRepositoryImpl(database)
    }

    @BeforeEach
    fun setUp(){

        transaction(database) {
            SchemaUtils.drop(SessionTable)
            SchemaUtils.create(SessionTable)
        }
    }
    @AfterEach
    fun tearDown(){
        transaction { SchemaUtils.drop(SessionTable) }
    }

    @Test
    fun `should save session`() = runTest {
        val sessionID = UUID.randomUUID().toString().replace("-", "")
        val userPhone = "89036559989"
        val sessionExpiration = Clock.System.now().toEpochMilliseconds() + 1000 * 60 * 60 * 24
        val session = APISession(
            phone = userPhone,
            sessionExpiration
        )
        val sessionValue = Json.Default.encodeToString(session)
        sessionRepository.upsert(id = sessionID, value = sessionValue, phone = userPhone)

        val savedSessionValue = sessionRepository.get(sessionID)!!
        assertEquals(sessionValue, savedSessionValue)
    }

    @Test
    fun `should delete session`() = runTest {
        val sessionID = UUID.randomUUID().toString().replace("-", "")
        val userPhone = "89036559989"
        val sessionExpiration = Clock.System.now().toEpochMilliseconds() + 1000 * 60 * 60 * 24
        val session = APISession(
            phone = userPhone,
            sessionExpiration
        )
        val sessionValue = Json.Default.encodeToString(session)
        sessionRepository.upsert(id = sessionID, value = sessionValue, phone = userPhone)
        //check session saved
        val savedSessionValue = sessionRepository.get(sessionID)!!
        assertEquals(sessionValue, savedSessionValue)
        //check session deleted
        sessionRepository.delete(sessionID)
        assertNull(sessionRepository.get(sessionID))
    }

    @Test
    fun `should delete all user sessions`() = runTest {
        val sessionID = UUID.randomUUID().toString().replace("-", "")
        val userPhone = "89036559989"
        val sessionExpiration = Clock.System.now().toEpochMilliseconds() + 1000 * 60 * 60 * 24
        val session = APISession(
            phone = userPhone,
            sessionExpiration
        )
        val sessionValue = Json.Default.encodeToString(session)
        sessionRepository.upsert(id = sessionID, value = sessionValue, phone = userPhone)

        //check session saved
        val savedSessionValue = sessionRepository.get(sessionID)!!
        assertEquals(sessionValue, savedSessionValue)
        //check sessions deleted
        sessionRepository.deleteAllUserSessions(userPhone)
        assertNull(sessionRepository.get(sessionID))
    }
}