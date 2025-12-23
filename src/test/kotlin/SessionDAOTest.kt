package com.mapprjct

import com.mapprjct.database.dao.SessionDAO
import com.mapprjct.database.daoimpl.SessionDAOImpl
import com.mapprjct.database.tables.SessionTable
import com.mapprjct.model.APISession
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Schema
import org.jetbrains.exposed.sql.SchemaUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Testcontainers
class SessionDAOTest(){
    companion object {
        @Container
        val postgreSQLContainer = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testDb")
            .withUsername("postgres")
            .withPassword("test")
            .withReuse(true)
    }

    private lateinit var database: Database
    private lateinit var sessionDAO: SessionDAO
    @BeforeEach
    fun setUp(){
        database = Database.connect(
            url = postgreSQLContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = "postgres",
            password = "test",
        )
        SchemaUtils.drop(
            SessionTable
        )
        SchemaUtils.create(
            SessionTable
        )
        sessionDAO = SessionDAOImpl(database)
    }
    @AfterEach
    fun tearDown(){
        SchemaUtils.drop(
            SessionTable
        )
    }

    @Test
    fun `should save session`() = runTest{
        val sessionID = UUID.randomUUID().toString().replace("-","")
        val userPhone = "89036559989"
        val sessionExpiration = Clock.System.now().toEpochMilliseconds() + 1000*60*60*24
        val session = APISession(
            phone = userPhone,
            sessionExpiration
        )
        val sessionValue = Json.encodeToString(session)
        sessionDAO.upsert(id = sessionID, value = sessionValue, phone = userPhone)

        val savedSessionValue = sessionDAO.get(sessionID)!!
        assertEquals(sessionValue, savedSessionValue)
    }
}
