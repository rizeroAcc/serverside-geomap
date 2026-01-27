package com.mapprjct.junit.repository

import com.mapprjct.builders.createInvitation
import com.mapprjct.builders.createTestUser
import com.mapprjct.database.daoimpl.InvitationRepositoryImpl
import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.repositoryImpl.ProjectRepositoryImpl
import com.mapprjct.database.repositoryImpl.UserRepositoryImpl
import com.mapprjct.database.tables.InviteCodeTable
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.database.tables.UserTable
import com.mapprjct.model.Invitation
import com.mapprjct.model.dto.Project
import com.mapprjct.model.Role
import com.mapprjct.model.dto.User
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*
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


    suspend fun createAndRegisterNewUser(phone : String) : User {
        val user = createTestUser { this.phone = phone }
        userRepository.insert(user = user, password = UUID.randomUUID().toString())
        return user
    }

    suspend fun createProject(ownerPhone : String, name : String) : Project {
        val project = projectRepository.insertProject(
            creatorPhone = ownerPhone,
            projectName = name
        )
        return project
    }

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
    }

    @BeforeEach
    fun setUp() {
        transaction(database) {
            SchemaUtils.drop(UserTable, ProjectTable, ProjectUsersTable,InviteCodeTable)
            SchemaUtils.create(UserTable, ProjectTable, ProjectUsersTable,InviteCodeTable)
        }
    }



    @Test
    fun `should receive existing invitation`() = runTest{
        suspendTransaction {
            //given
            val inviterUser = createAndRegisterNewUser("89036559989")
            val project = createProject(ownerPhone = inviterUser.phone, "test")
            val invitationCode = UUID.randomUUID()
            val expireAt = Clock.System.now().toEpochMilliseconds() + 1000*60*60*24
            InviteCodeTable.insert{
                it[InviteCodeTable.inviterPhone] = inviterUser.phone
                it[InviteCodeTable.inviteCode] = invitationCode
                it[InviteCodeTable.projectID] = UUID.fromString(project.projectID)
                it[InviteCodeTable.role] = Role.Admin.toShort()
                it[InviteCodeTable.expireAt] = expireAt
            }
            //when
            val invitation = invitationRepository.getInvitation(invitationCode)
            //then
            assertThat(invitation)
                .isNotNull().extracting { it!! }
                .satisfies(
                    { it.expireAt == expireAt},
                    { it.inviteCode == invitationCode }
                )
        }
    }

    @Test
    fun `should insert invitation`() = runTest {
        suspendTransaction {
            //given
            val inviterUser = createAndRegisterNewUser("89036559989")
            val project = createProject(ownerPhone = inviterUser.phone, "test")
            val invitation = createInvitation {
                fromInviter(inviterUser)
                toProject(project)
                withRole(Role.Worker)
            }
            //when
            val insertedInvitation = invitationRepository.insertInvitation(invitation).getOrNull()
            //then
            assertThat(insertedInvitation)
                .isNotNull()
                .isEqualTo(invitation)
        }
    }

    @Test
    fun `should return error, when inserting over 5 invitation per user`() = runTest {
        suspendTransaction {
            //given
            val inviterUser = createAndRegisterNewUser("89036559989")
            val project = createProject(ownerPhone = inviterUser.phone, "test")
            //prepare 5 invitations
            for (i in 1..5) {
                val invitation = Invitation(
                    inviterPhone = inviterUser.phone,
                    inviteCode = UUID.randomUUID(),
                    projectID = UUID.fromString(project.projectID),
                    expireAt = Clock.System.now().toEpochMilliseconds() + 1000*60*60*24,
                    role = Role.Worker,
                )
                invitationRepository.insertInvitation(invitation)
            }
            val invitation = Invitation(
                inviterPhone = inviterUser.phone,
                inviteCode = UUID.randomUUID(),
                projectID = UUID.fromString(project.projectID),
                expireAt = Clock.System.now().toEpochMilliseconds() + 1000*60*60*24,
                role = Role.Worker,
            )
            //when
            val result = invitationRepository.insertInvitation(invitation)
            //then
            assertThat(result.exceptionOrNull())
                .isNotNull()
                .isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Test
    fun `should delete existing invitation`() = runTest {
        suspendTransaction {
            //given
            val inviterUser = createAndRegisterNewUser("89036559989")
            val invitedUser = createAndRegisterNewUser("89038518685")
            val project = createProject(ownerPhone = inviterUser.phone, "test")
            val invitation = Invitation(
                inviterPhone = invitedUser.phone,
                inviteCode = UUID.randomUUID(),
                projectID = UUID.fromString(project.projectID),
                expireAt = Clock.System.now().toEpochMilliseconds() + 1000*60*60*24,
                role = Role.Worker,
            )
            val insertedInvitationCode = invitationRepository.insertInvitation(invitation).getOrNull()!!.inviteCode
            //check invitation inserted
            assertNotNull(invitationRepository.getInvitation(insertedInvitationCode))

            //when
            invitationRepository.deleteInvitation(invitation.inviteCode)
            //then
            assertNull(invitationRepository.getInvitation(insertedInvitationCode))
        }
    }
}