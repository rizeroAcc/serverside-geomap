package com.mapprjct.kotest.repository

import com.mapprjct.builders.createInvitation
import com.mapprjct.builders.createTestUser
import com.mapprjct.database.daoimpl.InvitationRepositoryImpl
import com.mapprjct.database.repositoryImpl.ProjectRepositoryImpl
import com.mapprjct.database.repositoryImpl.UserRepositoryImpl
import com.mapprjct.database.tables.InviteCodeTable
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.database.tables.UserTable
import com.mapprjct.model.dto.Role
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.testcontainers.containers.PostgreSQLContainer
import java.util.*

class InvitationRepositoryTest : FunSpec({
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
    val userRepository by lazy { UserRepositoryImpl(database) }
    val projectRepository by lazy { ProjectRepositoryImpl(database) }
    val invitationRepository by lazy { InvitationRepositoryImpl(database) }

    beforeSpec {
        suspendTransaction(database) {
            SchemaUtils.create(UserTable, ProjectTable, ProjectUsersTable,InviteCodeTable)
        }
    }
    beforeTest {
        suspendTransaction(database) {
            InviteCodeTable.deleteAll()
        }
    }
    context("with user and project") {

        val inviterUser = suspendTransaction(database) {
            createTestUser().also {
                userRepository.insert(it, "testPass")
            }
        }
        val project = suspendTransaction(database) {
            projectRepository.insertProject(inviterUser.phone, "testProject")
        }

        context("insert"){
            test("should insert invitations") {
                suspendTransaction(database) {
                    val invitation = createInvitation {
                        fromInviter(inviterUser)
                        toProject(project)
                        withRole(Role.Worker)
                    }
                    invitationRepository.insertInvitation(invitation).getOrNull() shouldNotBeNull{
                        this.projectID shouldBe invitation.projectID
                        this.inviterPhone shouldBe inviterUser.phone
                        this.inviteCode shouldBe invitation.inviteCode
                        this.expireAt shouldBe invitation.expireAt
                        this.role shouldBe invitation.role
                    }
                }
            }
            test("should return error, when inserting over 5 invitation per user"){
                suspendTransaction(database) {
                    for (i in 1..5){
                        createInvitation {
                            fromInviter(inviterUser)
                            toProject(project)
                            withRole(Role.Worker)
                        }.also {
                            invitationRepository.insertInvitation(it)
                        }
                    }
                    val overFiveInvitation = createInvitation {
                        fromInviter(inviterUser)
                        toProject(project)
                        withRole(Role.Worker)
                    }
                    shouldThrow<IllegalStateException>{
                        invitationRepository.insertInvitation(overFiveInvitation).getOrThrow()
                    }
                }
            }
        }
        context("get"){
            test("should get existing invitations") {
                suspendTransaction(database) {
                    val insertedInvitation = createInvitation {
                        fromInviter(inviterUser)
                        toProject(project)
                        withRole(Role.Worker)
                    }.also{
                        invitationRepository.insertInvitation(it).isSuccess shouldBe true
                    }
                    invitationRepository.getInvitation(insertedInvitation.inviteCode) shouldBe insertedInvitation
                }
            }
            test("should get null if invitation with code doesn't exist") {
                suspendTransaction(database) {
                    invitationRepository.getInvitation(UUID.randomUUID()) shouldBe null
                }
            }
        }
        context("delete") {
            test("should delete existing invitation"){
                suspendTransaction {
                    //given
                    val invitation = createInvitation {
                        fromInviter(inviterUser)
                        toProject(project)
                        withRole(Role.Worker)
                    }
                    invitationRepository.insertInvitation(invitation).getOrNull() shouldBe invitation
                    invitationRepository.deleteInvitation(invitation.inviteCode)
                    invitationRepository.getInvitation(invitation.inviteCode) shouldBe null
                }
            }
        }
    }
})