package com.mapprjct.repository

import com.mapprjct.database.dao.InvitationRepository
import com.mapprjct.database.dao.ProjectRepository
import com.mapprjct.database.dao.UserRepository
import com.mapprjct.database.daoimpl.InvitationRepositoryImpl
import com.mapprjct.database.daoimpl.ProjectRepositoryImpl
import com.mapprjct.database.daoimpl.UserRepositoryImpl
import com.mapprjct.database.tables.InviteCodeTable
import com.mapprjct.database.tables.InviteCodeTable.inviteCode
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.database.tables.UserTable
import com.mapprjct.dto.Project
import com.mapprjct.dto.Role
import com.mapprjct.dto.User
import com.mapprjct.model.Invitation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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

    @AfterEach
    fun tearDown() {
        transaction(database) {
            SchemaUtils.drop(InviteCodeTable)
        }
    }
    @Test
    fun `should receive existing invitation`() = runTest{
        val invitationCode = UUID.randomUUID()
        val expireAt = Clock.System.now().toEpochMilliseconds() + 1000*60*60*24
        transaction(database) {
            InviteCodeTable.insert{
                it[InviteCodeTable.inviterPhone] = invitedUser.phone
                it[InviteCodeTable.inviteCode] = invitationCode
                it[InviteCodeTable.projectID] = UUID.fromString(project.projectID)
                it[InviteCodeTable.role] = Role.Admin.toShort()
                it[InviteCodeTable.expireAt] = expireAt
            }
        }


    }

    @Test
    fun `should insert invitation`() = runTest {
        val invitation = Invitation(
            inviterPhone = invitedUser.phone,
            inviteCode = UUID.randomUUID(),
            projectID = UUID.fromString(project.projectID),
            expireAt = Clock.System.now().toEpochMilliseconds() + 1000*60*60*24,
            role = Role.Worker,
        )
        invitationRepository.insertInvitationCode(invitation)
    }

}