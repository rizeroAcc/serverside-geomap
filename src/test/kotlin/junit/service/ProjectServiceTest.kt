@file:OptIn(ExperimentalTime::class)

package com.mapprjct.junit.service

import com.mapprjct.AppConfig
import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.tables.InviteCodeTable
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.database.tables.UserTable
import com.mapprjct.di.repositoryModule
import com.mapprjct.di.serviceModule
import com.mapprjct.di.storageModule
import com.mapprjct.model.Invitation
import com.mapprjct.model.dto.Project
import com.mapprjct.model.Role
import com.mapprjct.model.dto.User
import com.mapprjct.service.ProjectService
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
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class ProjectServiceTest : KoinTest {
    companion object {
        @Container
        val postgreSQLContainer = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testDb")
            .withUsername("postgres")
            .withPassword("test")
            .withReuse(true)
    }

    private lateinit var database: Database
    private val userRepository: UserRepository by inject()
    private val invitationRepository: InvitationRepository by inject()
    private val projectService: ProjectService by inject()

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

    private suspend fun setupUsersAndProject(): Triple<User, User, Project> = suspendTransaction {
        val owner = createUser("89036559989", "test")
        val invited = createUser("89038518685", "user")
        val project = projectService.createProject(owner.phone, "test1").getOrThrow()

        Triple(owner, invited, project)
    }

    private suspend fun createAndSaveInvitation(owner: User, project: Project): Invitation = suspendTransaction {
        val invitation = Invitation(
            inviterPhone = owner.phone,
            inviteCode = UUID.randomUUID(),
            projectID = UUID.fromString(project.projectID),
            expireAt = Clock.System.now().toEpochMilliseconds() + 86_400_000, // 24 часа
            role = Role.Worker
        )
        invitationRepository.insertInvitation(invitation)
        invitation
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
    inner class CreateProject {
        @Test
        fun `should create new project`() = runTest {
            //given
            val projectName = "testProject"
            val user = createUser("89036559989","test")
            suspendTransaction(database) {
                //when
                val createdProject = projectService.createProject(
                    user.phone, projectName
                ).getOrThrow()
                //then
                ProjectTable.selectAll().where {
                    ProjectTable.id eq UUID.fromString(createdProject.projectID)
                }.single().let {
                    val storedProject = Project(
                        projectID = it[ProjectTable.id].toString(),
                        name = it[ProjectTable.name],
                        membersCount = it[ProjectTable.membersCount].toInt()
                    )
                    assertThat(storedProject).isEqualTo(createdProject)
                }
            }
        }
        @Test
        fun `should return UserNotFoundException when user does not exist`() = runTest {
            //given
            val wrongPhone = "89036559999"
            suspendTransaction(database) {
                //when
                val exception = projectService.createProject(wrongPhone,"test")
                    .exceptionOrNull()
                //then
                assertThat(exception)
                    .isNotNull()
                    .isInstanceOf(UserDMLExceptions.UserNotFoundException::class.java)
            }
        }
    }

    @Nested
    inner class GetUserProjects {
        @Test
        fun `should get all user projects`() = runTest {
            suspendTransaction(database) {
                val user = createUser("89036559989","test")
                //given
                val userProjects = listOf(
                    projectService.createProject(user.phone,"test1").getOrThrow(),
                    projectService.createProject(user.phone,"test2").getOrThrow(),
                    projectService.createProject(user.phone,"test3").getOrThrow()
                )
                //when
                val receivedUserProjects = projectService.getAllUserProjects(user.phone).getOrThrow()
                //then
                receivedUserProjects.forEachIndexed { index, projectWithRole ->
                    assertThat(projectWithRole.project)
                        .isEqualTo(userProjects[index])
                }
            }
        }
        @Test
        fun `should return UserNotFoundException if user does not exist`() = runTest {
            //given
            val wrongPhone = "89036559999"
            suspendTransaction(database) {
                //when
                val exception = projectService.getAllUserProjects(wrongPhone)
                    .exceptionOrNull()
                //then
                assertThat(exception)
                    .isNotNull()
                    .isInstanceOf(UserDMLExceptions.UserNotFoundException::class.java)
            }
        }
    }

    @Nested
    inner class JoinProject{
        @Test
        fun `should add user to project by invitation and delete invitation`() = runTest {
            suspendTransaction {
                //given
                val (owner, invitedUser, project) = setupUsersAndProject()
                val invitation = createAndSaveInvitation(owner, project)
                //when
                projectService.joinProject(
                    userPhone = invitedUser.phone,
                    invitationCode = invitation.inviteCode.toString()
                )
                //then
                val invitedUserProjects = projectService.getAllUserProjects(invitedUser.phone).getOrThrow()
                assertThat(invitedUserProjects[0].project.projectID)
                    .isEqualTo(project.projectID)
                assertThat(invitationRepository.getInvitation(invitation.inviteCode))
                    .isNull()
            }
        }
        @Test
        fun `should return InvitationNotFoundException if invitation doesn't exist`() = runTest {
            suspendTransaction {
                //given
                val user = createUser("89036559989", "test")
                //when
                val result = projectService.joinProject(user.phone, UUID.randomUUID().toString())
                //then
                assertThat(result.exceptionOrNull())
                    .isInstanceOf(InvitationDMLExceptions.InvitationNotFoundException::class.java)
            }
        }
        @Test
        fun `should return UserAlreadyProjectMemberException if user already project member`() = runTest {
            suspendTransaction {
                //given
                val (owner, invitedUser, project) = setupUsersAndProject()
                val invitation = createAndSaveInvitation(owner, project)
                //when
                val result = projectService.joinProject(
                    userPhone = owner.phone,
                    invitationCode = invitation.inviteCode.toString()
                )
                //then
                assertThat(result.exceptionOrNull())
                    .isInstanceOf(ProjectValidationException.UserAlreadyProjectMember::class.java)
            }
        }
    }
}