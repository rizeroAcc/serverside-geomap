package com.mapprjct.kotest.repository

import com.mapprjct.builders.createCredentials
import com.mapprjct.builders.createTestUser
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.repositoryImpl.UserRepositoryImpl
import com.mapprjct.database.tables.UserTable
import com.mapprjct.model.datatype.Password
import com.mapprjct.model.datatype.RussiaPhoneNumber
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.postgresql.util.PSQLState
import org.testcontainers.containers.PostgreSQLContainer

class UserRepositoryTest : FunSpec({
    val postgres = PostgreSQLContainer("postgres:latest")
        .withDatabaseName("testdb")
        .withUsername("postgres")
        .withPassword("postgres")
        .withReuse(true)

    install(TestContainerSpecExtension(postgres))

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
        val validPassword = Password("validPassword")
        test("should insert user") {
            val user = createTestUser()
            suspendTransaction(database) {
                userRepository.insert(user, validPassword) shouldBe user
            }
            //check user pass saved
            transaction(database) {
                UserTable.selectAll()
                    .where { UserTable.phone eq user.phone.normalizeAsRussiaPhone() }
                    .single()
                    .also {
                        it[UserTable.passwordHash] shouldBe validPassword.value
                    }
            }
        }
        test("should throw unique constraint violation if user already exists") {
            val user = createTestUser().also {
                suspendTransaction(database) { userRepository.insert(it, validPassword) }
            }
            suspendTransaction(database) {
                shouldThrow<ExposedSQLException> {
                    userRepository.insert(user, validPassword)
                }.sqlState shouldBe PSQLState.UNIQUE_VIOLATION.state
            }
        }
    }

    context("find user"){
        test("should receive existing user") {
            val user = createTestUser().also {
                suspendTransaction { userRepository.insert(it, Password("testPassword")) }
            }
            suspendTransaction(database) {
                userRepository.findUser(user.phone) shouldBe user
            }
        }
        test("should return null if user does not exist"){
            val unexistedUserPhone = RussiaPhoneNumber("89036559989")
            suspendTransaction(database) {
                userRepository.findUser(unexistedUserPhone) shouldBe null
            }
        }
    }
    context("update user"){
        test("should update user fields except phone") {
            createTestUser().also {
                suspendTransaction(database) { userRepository.insert(it, Password("testPassword")) }
            }
            val updatedUserInfo = createTestUser {
                username = "updatedUserName"
                avatarFilename = "updatedUserAvatarFilename"
            }
            suspendTransaction(database) {
                userRepository.updateUser(updatedUserInfo) shouldBe updatedUserInfo
            }
        }
        test("should return null if updatable user does not exist"){
            val user = createTestUser()
            suspendTransaction(database) {
                userRepository.updateUser(user) shouldBe null
            }
        }
    }
    context("update password"){
        test("should update user password"){
            val password = Password("testPassword")
            val newPassword = Password("newPassword")
            val user = createTestUser {
                phone = "89036559989"
                username = "userName"
            }
            suspendTransaction(database) {
                userRepository.insert(user, password)
                userRepository.updateUserPassword(userPhone = user.phone, password = newPassword) shouldBe newPassword
            }
        }
        test("should return null if updatable user does not exist"){
            val unregisteredUser = createTestUser()
            val newPassword = Password("newPassword")
            suspendTransaction {
                userRepository.updateUserPassword(unregisteredUser.phone, newPassword) shouldBe null
            }
        }
    }
    context("user credentials"){
        test("should receive user credentials"){
            //given
            val user = createTestUser()
            val userPassword = Password("testPassword")
            val userCredentials = createCredentials {
                forUser(user)
                password = userPassword.value
            }
            suspendTransaction(database) {
                userRepository.insert(user, userPassword)
                userRepository.getUserCredentials(user.phone) shouldBe userCredentials
            }
        }
        test("should receive null when user doesn't exist"){
            //given
            val unexistedUserPhone = RussiaPhoneNumber("89036559989")
            suspendTransaction(database) {
                userRepository.getUserCredentials(unexistedUserPhone) shouldBe null
            }
        }
    }
}
)