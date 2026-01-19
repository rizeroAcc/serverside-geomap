package com.mapprjct.junit.service

import com.mapprjct.AppConfig
import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.tables.InviteCodeTable
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.database.tables.UserTable
import com.mapprjct.di.repositoryModule
import com.mapprjct.di.serviceModule
import com.mapprjct.di.storageModule
import com.mapprjct.exceptions.invitation.InvitationValidationException
import com.mapprjct.exceptions.project.ProjectDMLException
import com.mapprjct.model.Invitation
import com.mapprjct.model.dto.Role
import com.mapprjct.model.dto.User
import com.mapprjct.model.dto.asRole
import com.mapprjct.service.InvitationService
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.logger.slf4jLogger
import org.koin.test.KoinTest
import org.koin.test.junit5.KoinTestExtension
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InvitationServiceTest : KoinTest {
    companion object {
        @Container
        val postgreSQLContainer = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testDb")
            .withUsername("postgres")
            .withPassword("test")
            .withReuse(true)
    }

    private lateinit var database: Database
    private val userRepository : UserRepository by inject()
    private val projectRepository: ProjectRepository by inject()
    private val invitationService: InvitationService by inject()

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
        startKoin {
            slf4jLogger()
            modules(
                module {
                    single { database }
                    single { AppConfig.Test }
                       },
                storageModule,
                repositoryModule,
                serviceModule
            )
        }
    }

    @AfterAll
    fun shutdown() {
        stopKoin()
    }

    @BeforeEach
    fun setUp() = runBlocking {
        suspendTransaction(database) {
            SchemaUtils.drop(UserTable, ProjectTable, ProjectUsersTable, InviteCodeTable)
            SchemaUtils.create(UserTable, ProjectTable, ProjectUsersTable, InviteCodeTable)
        }
    }

    @Nested
    inner class CreateInvitation {
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

        @Test
        fun `should return IllegalArgumentException if projectID invalid`() = runTest {
            val invalidID = UUID.randomUUID().toString().replace("-", "")
            val result = invitationService.createInvitation(
                inviterPhone = "89036559989",
                projectID = invalidID,
                role = Role.Worker.toShort()
            )
            assertThat(result.exceptionOrNull())
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `should return IllegalArgumentException if role is Invalid`() = runTest {
            suspendTransaction {
                val user = createUser(phone ="89036559989", name ="test",password = "test")
                val project = projectRepository.insertProject(user.phone,"test")
                val invalidRoleCode = 5.toShort()
                val result = invitationService.createInvitation(
                    inviterPhone = "89036559989",
                    projectID = project.projectID,
                    role = invalidRoleCode
                )
                assertThat(result.exceptionOrNull())
                    .isInstanceOf(IllegalArgumentException::class.java)
            }
        }

        @Test
        fun `should return InvitationValidationException if role is Owner`() = runTest {
            suspendTransaction {
                val user = createUser(phone ="89036559989", name ="test",password = "test")
                val project = projectRepository.insertProject(user.phone,"test")

                val result = invitationService.createInvitation(
                    inviterPhone = "89036559989",
                    projectID = project.projectID,
                    role = Role.Owner.toShort()
                )
                assertThat(result.exceptionOrNull())
                    .isInstanceOf(InvitationValidationException.InvalidUserRole::class.java)
            }

        }

        @Test
        fun `should return ProjectNotFoundException if project doesn't exists`() = runTest {
            suspendTransaction {
                val projectID = UUID.randomUUID().toString()
                val result = invitationService.createInvitation(
                    inviterPhone = "89036559989",
                    projectID = projectID,
                    role = Role.Worker.toShort()
                )
                assertThat(result.exceptionOrNull())
                    .isInstanceOf(ProjectDMLException.ProjectNotFoundException::class.java)
            }
        }

        @Test
        fun `should return IllegalArgumentException if user have over 5 invitations`() = runTest {
            suspendTransaction {
                val user = createUser(phone ="89036559989", name ="test",password = "test")
                val project = projectRepository.insertProject(user.phone,"test")
                for (i in 1..5){
                    invitationService.createInvitation(
                        inviterPhone = "89036559989",
                        projectID = project.projectID,
                        role = Role.Worker.toShort()
                    )
                }
                val result = invitationService.createInvitation(
                    inviterPhone = "89036559989",
                    projectID = project.projectID,
                    role = Role.Worker.toShort()
                )
                assertThat(result.exceptionOrNull())
                    .isInstanceOf(IllegalStateException::class.java)
            }
        }
    }
}