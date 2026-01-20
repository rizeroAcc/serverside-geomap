package com.mapprjct.kotest.service

import com.mapprjct.AppConfig
import com.mapprjct.database.storage.AvatarStorage
import com.mapprjct.database.tables.UserTable
import com.mapprjct.di.repositoryModule
import com.mapprjct.di.serviceModule
import com.mapprjct.di.storageModule
import com.mapprjct.exceptions.user.UserValidationException
import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.service.UserService
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.koin.KoinExtension
import io.kotest.koin.KoinLifecycleMode
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.testcontainers.containers.PostgreSQLContainer

class UserServiceTest : KoinTest, FunSpec(){
    init {
        val postgres = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testdb")
            .withUsername("postgres")
            .withPassword("postgres")
            .withReuse(true)

        val database by lazy {
            Database.connect(
                url = postgres.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = postgres.username,
                password = postgres.password
            )
        }

        install(TestContainerSpecExtension(postgres))
        extension(
            KoinExtension(
                modules = listOf(
                    module {
                        single { database }
                        single { AppConfig.Test }
                    },
                    storageModule,
                    repositoryModule,
                    serviceModule
                ),
                mode = KoinLifecycleMode.Root
            )
        )
        // Подключаемся один раз к контейнеру

        val userService:UserService by inject()
        val avatarStorage:AvatarStorage by inject()

        beforeSpec {
            suspendTransaction(database) {
                SchemaUtils.create(UserTable)
            }
        }
        beforeTest {
            suspendTransaction(database) {
                UserTable.deleteAll()
            }
        }
        context("create user") {
            test("should create user with valid info"){
                //given
                val credentials = UserCredentials("89036559989", "12345678")
                val username = "myUser"
                suspendTransaction {
                    //when
                    userService.createUser(userCredentials = credentials, username = username)
                    //then
                    UserTable.selectAll()
                        .where { UserTable.phone eq credentials.phone }
                        .single().asClue {
                        it[UserTable.phone] shouldBe credentials.phone
                        it[UserTable.passwordHash] shouldBe credentials.password
                        it[UserTable.username] shouldBe username
                    }
                }
            }
            test("should return validation error if new user phone incorrect"){
                //given
                val credentials = UserCredentials("890365599", "12345678")
                suspendTransaction {
                    shouldThrow<UserValidationException.InvalidPhoneFormat> {
                        userService.createUser(userCredentials = credentials, username = "test")
                            .getOrThrow()
                    }
                }
            }
            test("should return validation error if new user password length less 8 symbols"){
                //given
                val credentials = UserCredentials("89036559989", "1234567")
                suspendTransaction {
                    shouldThrow<UserValidationException.InvalidPasswordLength> {
                        userService.createUser(userCredentials = credentials, username = "test")
                            .getOrThrow()
                    }
                }
            }
            test("should return validation error if new user username empty"){
                val credentials = UserCredentials("89036559989", "12345678")
                suspendTransaction {
                    shouldThrow<UserValidationException.InvalidUsername> {
                        userService.createUser(userCredentials = credentials, username = "    ")
                            .getOrThrow()
                    }
                }
            }
        }
    }
}