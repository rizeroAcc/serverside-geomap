package com.mapprjct.service

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
import com.mapprjct.model.dto.Role
import com.mapprjct.model.dto.User
import com.mapprjct.model.dto.asRole
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class InvitationServiceTest {
    companion object {
        @Container
        val postgreSQLContainer = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testDb")
            .withUsername("postgres")
            .withPassword("test")
            .withReuse(true)
    }

    private lateinit var database: Database

    private lateinit var userRepository: UserRepository
    private lateinit var projectRepository: ProjectRepository
    private lateinit var invitationRepository: InvitationRepository

    private lateinit var invitationService: InvitationService

    private fun createUser(
        phone : String,
        name : String,
        password : String = "test",
        avatar: String? = null) : User {
        return runBlocking {
            suspendTransaction {
                val user = User (phone,name,avatar)
                userRepository.insert(user,password)
                user
            }
        }
    }

    @BeforeAll
    fun initialize() {
        database = Database.connect(
            url = postgreSQLContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgreSQLContainer.username,
            password = postgreSQLContainer.password,
        )
        userRepository = UserRepositoryImpl(database)
        projectRepository = ProjectRepositoryImpl(database)
        invitationRepository = InvitationRepositoryImpl(database)
        invitationService = InvitationService(
            projectRepository = projectRepository,
            invitationRepository = invitationRepository
        )
    }

    @BeforeEach
    fun setUp() = runBlocking {
        suspendTransaction(database) {
            SchemaUtils.drop(UserTable, ProjectTable, ProjectUsersTable, InviteCodeTable)
            SchemaUtils.create(UserTable, ProjectTable, ProjectUsersTable, InviteCodeTable)
        }
    }

    @Test
    fun `should create invitation`() = runTest {
        suspendTransaction {
            val user = createUser(phone ="89036559989", name ="test",password = "test")
            val project = projectRepository.insertProject(user.phone,"test")
            val invitation = invitationService.createInvitation(
                inviterPhone = user.phone,
                projectID = project.projectID,
                role = Role.Worker.toShort()
            ).getOrThrow()
            InviteCodeTable.selectAll().where {
                InviteCodeTable.inviteCode eq invitation.inviteCode
            }.single().let {
                val insertedInvitation = Invitation(
                    inviterPhone = it[InviteCodeTable.inviterPhone],
                    inviteCode = it[InviteCodeTable.inviteCode],
                    projectID = it[InviteCodeTable.projectID],
                    expireAt = it[InviteCodeTable.expireAt],
                    role = it[InviteCodeTable.role].asRole()
                )
                assertThat(insertedInvitation).isEqualTo(invitation)
            }
        }
    }

    //todo bad scenario
}