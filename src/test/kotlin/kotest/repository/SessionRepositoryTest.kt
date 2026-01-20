package com.mapprjct.kotest.repository

import com.mapprjct.database.daoimpl.SessionRepositoryImpl
import com.mapprjct.database.tables.SessionTable
import com.mapprjct.model.APISession
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.testcontainers.containers.PostgreSQLContainer
import java.util.*

class SessionRepositoryTest : FunSpec({
    val postgres = PostgreSQLContainer("postgres:latest")
        .withDatabaseName("testdb")
        .withUsername("postgres")
        .withPassword("postgres")
        .withReuse(true)
    install(TestContainerSpecExtension(postgres))

    // Подключаемся один раз к контейнеру
    val database by lazy {
        Database.connect(
            url = postgres.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgres.username,
            password = postgres.password
        )
    }
    val sessionRepository by lazy { SessionRepositoryImpl(database) }

    beforeSpec {
        suspendTransaction(database) {
            SchemaUtils.create(SessionTable)
        }
    }
    beforeEach {
        suspendTransaction {
            SessionTable.deleteAll()
        }
    }

    context("save and get session") {
        test("should save and get session") {
            val sessionID = UUID.randomUUID().toString().replace("-", "")
            val userPhone = "89036559989"
            val session = APISession(
                phone = userPhone,
                expireAt = 86_400_000L,
            )
            val sessionValue = Json.encodeToString(session)
            sessionRepository.upsert(id = sessionID, value = sessionValue, phone = userPhone)
            sessionRepository.get(sessionID)!! shouldBe sessionValue

        }
    }
    context("remove session"){
        test("should delete session"){
            val sessionID = UUID.randomUUID().toString().replace("-", "")
            val userPhone = "89036559989"
            val session = APISession(
                phone = userPhone,
                expireAt = 86_400_000L,
            )
            val sessionValue = Json.encodeToString(session)
            sessionRepository.upsert(id = sessionID, value = sessionValue, phone = userPhone)
            //check session deleted
            sessionRepository.delete(sessionID)
            sessionRepository.get(sessionID) shouldBe null
        }
        test("should delete all user sessions"){
            suspendTransaction {
                val sessionOneID = UUID.randomUUID().toString().replace("-", "")
                val sessionTwoID = UUID.randomUUID().toString().replace("-", "")
                val userPhone = "89036559989"
                val session = APISession(
                    phone = userPhone,
                    expireAt = 86_400_000L,
                )
                val sessionValue = Json.encodeToString(session)
                sessionRepository.upsert(id = sessionOneID, value = sessionValue, phone = userPhone)
                sessionRepository.upsert(id = sessionTwoID, value = sessionValue, phone = userPhone)
                //check sessions deleted
                sessionRepository.deleteAllUserSessions(userPhone)
                sessionRepository.get(sessionOneID) shouldBe null
                sessionRepository.get(sessionTwoID) shouldBe null
            }
        }
    }

})