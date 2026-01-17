package com.mapprjct.kotest.repository

import com.mapprjct.builders.createCredentials
import com.mapprjct.builders.createTestUser
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.repositoryImpl.UserRepositoryImpl
import com.mapprjct.database.tables.UserTable
import com.mapprjct.model.dto.UserCredentials
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldHave
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer

class UserRepositoryTest : FunSpec({
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

    val userRepository : UserRepository by lazy { UserRepositoryImpl(database) }

    beforeEach {
        transaction(database) {
            SchemaUtils.drop(UserTable)
            SchemaUtils.create(UserTable)
        }
    }

    context("insert") {
        test("should insert user") {
            // given
            val user = createTestUser()
            val password = "testPassword"

            // when
            suspendTransaction(database) {
                userRepository.insert(user, password)
            }

            // then
            transaction(database) {
                val row = UserTable.selectAll()
                    .where { UserTable.phone eq user.phone }
                    .single()

                row[UserTable.phone] shouldBe user.phone
                row[UserTable.username] shouldBe user.username
                row[UserTable.passwordHash] shouldBe password
                row[UserTable.avatar] shouldBe user.avatarFilename
            }
        }
        test("should throw unique constraint violation if phone already exists") {
            val user = createTestUser()
            val password = "testPassword"

            suspendTransaction(database) {
                userRepository.insert(user, password)

                val exception = shouldThrow<ExposedSQLException> {
                    userRepository.insert(user, password)
                }

                exception.sqlState shouldBe "23505"  // unique violation
            }
        }
        test("should throw check constraint violation if phone invalid") {
            val invalidUser = createTestUser { phone = "8903655998" }  // короткий телефон

            suspendTransaction(database) {
                val exception = shouldThrow<ExposedSQLException> {
                    userRepository.insert(invalidUser, "test")
                }
                exception.sqlState shouldBe "23514"  // check violation
            }
        }
        test("should throw check constraint violation if username empty") {
            val emptyUser = createTestUser { username = "" }

            suspendTransaction(database) {
                val exception = shouldThrow<ExposedSQLException> {
                    userRepository.insert(emptyUser, "test")
                }
                exception.sqlState shouldBe "23514"
            }
        }
    }

    context("get user"){
        test("should receive existing user") {
            val user = createTestUser()
            suspendTransaction(database) {
                userRepository.insert(user, "test")
                userRepository.getUser(user.phone) shouldBe user
            }
        }
        test("should return null if user does not exist"){
            val unexistedUserPhone = "89036559989"
            suspendTransaction(database) {
                userRepository.getUser(unexistedUserPhone) shouldBe null
            }
        }
    }
    context("update user"){
        test("should update user fields except phone and password") {
            val user = createTestUser()
            val updatedUserInfo = createTestUser {
                username = "updatedUserName"
                avatarFilename = "updatedUserAvatarFilename"
            }
            suspendTransaction(database) {
                userRepository.insert(user, "testPassword")
                userRepository.updateUser(updatedUserInfo) shouldBe updatedUserInfo
            }
        }
    }
    context("uesr credentials"){
        test("should receive user credentials"){
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
                userRepository.getUserCredentials("89036559989") shouldBe expectedCredentials
            }
        }
        test("should receive null instead credentials when user doesn't exist"){
            //given
            val unexistedUserPhone = "89036559989"
            suspendTransaction(database) {
                userRepository.getUserCredentials(unexistedUserPhone) shouldBe null
            }
        }
        test("should update user password"){
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
                userRepository.getUserCredentials(user.phone) shouldNotBeNull {
                    this.password shouldBe newPassword
                }
            }
        }
    }
}
)