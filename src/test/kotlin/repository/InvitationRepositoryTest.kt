package com.mapprjct.repository

import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.daoimpl.InvitationRepositoryImpl
import com.mapprjct.database.repositoryImpl.ProjectRepositoryImpl
import com.mapprjct.database.repositoryImpl.UserRepositoryImpl
import com.mapprjct.database.tables.InviteCodeTable
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.database.tables.UserTable
import com.mapprjct.model.dto.Project
import com.mapprjct.model.dto.Role
import com.mapprjct.model.dto.User
import com.mapprjct.model.Invitation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InvitationRepositoryTest {
    companion object {
        @Container
        val postgreSQLContainer = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testDb")
            .withUsername("postgres")
            .withPassword("test")
            .withReuse(true)
    }

    private lateinit var database: Database
    private lateinit var invitationRepository: InvitationRepository

    private lateinit var userRepository: UserRepository
    private lateinit var projectRepository: ProjectRepository

    val projectOwnerUser = User(phone = "89036559989", username = "test_user", avatarPath = null)
    val invitedUser = User(phone = "89038518685", username = "test_user2", avatarPath = null)
    lateinit var project: Project
    @BeforeAll
    fun initialize() {
        database = Database.connect(
            url = postgreSQLContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = "postgres",
            password = "test",
        )
        invitationRepository = InvitationRepositoryImpl(database)
        userRepository = UserRepositoryImpl(database)
        projectRepository = ProjectRepositoryImpl(database)

        transaction(database) {
            SchemaUtils.create(UserTable, ProjectTable, ProjectUsersTable)
            runBlocking {
                userRepository.insert(projectOwnerUser, "test_pass")
                userRepository.insert(invitedUser, "test_pass")
                project = projectRepository.insertProject(projectOwnerUser.phone, "test_project")
            }
        }

    }

    @BeforeEach
    fun setUp() {
        transaction(database) {
            SchemaUtils.drop(InviteCodeTable)
            SchemaUtils.create(InviteCodeTable)
        }
    }



    @Test
    fun `should receive existing invitation`() = runTest{
        suspendTransaction {
            //given
            val invitationCode = UUID.randomUUID()
            val expireAt = Clock.System.now().toEpochMilliseconds() + 1000*60*60*24
            InviteCodeTable.insert{
                it[InviteCodeTable.inviterPhone] = invitedUser.phone
                it[InviteCodeTable.inviteCode] = invitationCode
                it[InviteCodeTable.projectID] = UUID.fromString(project.projectID)
                it[InviteCodeTable.role] = Role.Admin.toShort()
                it[InviteCodeTable.expireAt] = expireAt
            }
            //when
            val invitation = invitationRepository.getInvitation(invitationCode)
            //then
            assertNotNull(invitation)
            assertEquals(expireAt, invitation.expireAt)
            assertEquals(invitationCode, invitation.inviteCode)
        }
    }

    @Test
    fun `should insert invitation`() = runTest {
        suspendTransaction {
            //given
            val invitation = Invitation(
                inviterPhone = invitedUser.phone,
                inviteCode = UUID.randomUUID(),
                projectID = UUID.fromString(project.projectID),
                expireAt = Clock.System.now().toEpochMilliseconds() + 1000*60*60*24,
                role = Role.Worker,
            )
            //when
            val insertedInvitation = invitationRepository.insertInvitationCode(invitation).getOrNull()
            //then
            assertNotNull(insertedInvitation)
            assertEquals(invitation, insertedInvitation)
        }
    }

    @Test
    fun `should return error, when inserting over 5 invitation per user`() = runTest {
        suspendTransaction {
            //given
            //prepare 5 invitations
            for (i in 1..5) {
                val invitation = Invitation(
                    inviterPhone = invitedUser.phone,
                    inviteCode = UUID.randomUUID(),
                    projectID = UUID.fromString(project.projectID),
                    expireAt = Clock.System.now().toEpochMilliseconds() + 1000*60*60*24,
                    role = Role.Worker,
                )
                invitationRepository.insertInvitationCode(invitation)
            }
            val invitation = Invitation(
                inviterPhone = invitedUser.phone,
                inviteCode = UUID.randomUUID(),
                projectID = UUID.fromString(project.projectID),
                expireAt = Clock.System.now().toEpochMilliseconds() + 1000*60*60*24,
                role = Role.Worker,
            )
            //when
            val insertedInvitation = invitationRepository.insertInvitationCode(invitation)
            //then
            assertNotNull(insertedInvitation.exceptionOrNull())
        }
    }

    @Test
    fun `should delete existing invitation`() = runTest {
        suspendTransaction {
            //given
            val invitation = Invitation(
                inviterPhone = invitedUser.phone,
                inviteCode = UUID.randomUUID(),
                projectID = UUID.fromString(project.projectID),
                expireAt = Clock.System.now().toEpochMilliseconds() + 1000*60*60*24,
                role = Role.Worker,
            )
            val insertedInvitationCode = invitationRepository.insertInvitationCode(invitation).getOrNull()!!.inviteCode
            //check invitation inserted
            assertNotNull(invitationRepository.getInvitation(insertedInvitationCode))

            //when
            invitationRepository.deleteInvitationCode(invitation)
            //then
            assertNull(invitationRepository.getInvitation(insertedInvitationCode))
        }
    }
}