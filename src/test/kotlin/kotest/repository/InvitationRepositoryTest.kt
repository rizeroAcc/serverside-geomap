package com.mapprjct.kotest.repository

import com.mapprjct.builders.createInvitation
import com.mapprjct.database.daoimpl.InvitationRepositoryImpl
import com.mapprjct.database.tables.InviteCodeTable
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.database.tables.UserTable
import com.mapprjct.model.datatype.Role
import com.mapprjct.withRegisteredProject
import com.mapprjct.withRegisteredUser
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
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
    val invitationRepository by lazy { InvitationRepositoryImpl() }

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
        withRegisteredUser { userPhone ->
            withRegisteredProject(userPhone) { projectID ->
                context("insert"){
                    test("should insert invitation") {
                        suspendTransaction(database) {
                            val invitation = createInvitation {
                                fromInviter(userPhone.normalizeAsRussiaPhone())
                                toProject(projectID)
                                withRole(Role.Worker)
                            }
                            invitationRepository.insertInvitation(invitation) shouldBe invitation
                        }
                    }
                }
                context("get"){
                    test("should get existing invitations") {
                        suspendTransaction(database) {
                            val insertedInvitation = createInvitation {
                                fromInviter(userPhone.normalizeAsRussiaPhone())
                                toProject(projectID)
                                withRole(Role.Worker)
                            }.also{
                                invitationRepository.insertInvitation(it) shouldBe it
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
                            val invitation = createInvitation {
                                fromInviter(userPhone.value)
                                toProject(projectID)
                                withRole(Role.Worker)
                            }
                            invitationRepository.insertInvitation(invitation)
                            invitationRepository.deleteInvitation(invitation.inviteCode)
                            invitationRepository.getInvitation(invitation.inviteCode) shouldBe null
                        }
                    }
                }
            }
        }
    }
})