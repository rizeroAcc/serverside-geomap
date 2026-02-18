package com.mapprjct.kotest.service

import com.mapprjct.AppConfig
import com.mapprjct.builders.createTestUser
import com.mapprjct.database.tables.InviteCodeTable
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.database.tables.UserTable
import com.mapprjct.di.repositoryModule
import com.mapprjct.di.serviceModule
import com.mapprjct.di.storageModule
import com.mapprjct.model.Role
import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.model.asRole
import com.mapprjct.service.InvitationService
import com.mapprjct.service.ProjectService
import com.mapprjct.service.UserService
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldNotThrowAny
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
import java.util.*

class InvitationServiceTest : KoinTest, FunSpec() {
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


        val userService: UserService by inject()
        val projectService: ProjectService by inject()
        val invitationService: InvitationService by inject()
        beforeSpec {
            suspendTransaction(database) {
                SchemaUtils.create(UserTable, ProjectTable, ProjectUsersTable, InviteCodeTable)
            }
        }
        beforeTest {
            suspendTransaction(database) {
                InviteCodeTable.deleteAll()
            }
        }

        context("create invitation") {
            val user = createTestUser {  }
            val userCredentials = UserCredentials(user.phone, "testPass")
            userService.createUser(userCredentials, user.username)
            val project = projectService.createProject(user.phone, "test").getOrThrow()
            test("should create invitation"){
                val invitation = shouldNotThrowAny {
                    invitationService.createInvitation(
                        inviterPhone = user.phone,
                        projectID = project.projectID,
                        role = Role.Worker.toShort()
                    ).getOrThrow()
                }
                suspendTransaction(database) {
                    InviteCodeTable.selectAll().where {
                        InviteCodeTable.inviteCode eq invitation.inviteCode
                    }.single().asClue {
                        it[InviteCodeTable.inviterPhone] shouldBe invitation.inviterPhone
                        it[InviteCodeTable.inviteCode] shouldBe invitation.inviteCode
                        it[InviteCodeTable.projectID] shouldBe invitation.projectID
                        it[InviteCodeTable.expireAt] shouldBe invitation.expireAt
                        it[InviteCodeTable.role].asRole() shouldBe invitation.role
                    }
                }
            }
            test("should return IllegalArgumentException if projectID invalid"){
                val invalidID = UUID.randomUUID().toString().replace("-", "")
                shouldThrow<IllegalArgumentException> {
                    invitationService.createInvitation(
                        inviterPhone = user.phone,
                        projectID = invalidID,
                        role = Role.Worker.toShort()
                    ).getOrThrow()
                }
            }
            test("should return IllegalArgumentException if role is Invalid"){
                val invalidRoleCode = 5.toShort()
                shouldThrow<IllegalArgumentException> {
                    invitationService.createInvitation(
                        inviterPhone = user.phone,
                        projectID = project.projectID,
                        role = invalidRoleCode
                    ).getOrThrow()
                }
            }
            test("should return InvalidUserRole if role is Owner"){
                shouldThrow<InvitationValidationException.InvalidUserRole> {
                    invitationService.createInvitation(
                        inviterPhone = user.phone,
                        projectID = project.projectID,
                        role = Role.Owner.toShort()
                    ).getOrThrow()
                }
            }
            test("should return ProjectNotFoundException if project doesn't exists"){
                val projectID = UUID.randomUUID().toString()
                shouldThrow<ProjectDMLException.ProjectNotFoundException> {
                    invitationService.createInvitation(
                        inviterPhone = "89036559989",
                        projectID = projectID,
                        role = Role.Worker.toShort()
                    ).getOrThrow()
                }
            }
        }
    }
}