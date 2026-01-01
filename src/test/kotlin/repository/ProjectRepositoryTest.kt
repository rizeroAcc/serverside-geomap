package com.mapprjct.dao

import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.repositoryImpl.ProjectRepositoryImpl
import com.mapprjct.database.repositoryImpl.UserRepositoryImpl
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.database.tables.UserTable
import com.mapprjct.model.dto.Project
import com.mapprjct.model.dto.Role
import com.mapprjct.model.dto.User
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertNotNull
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.assertEquals

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProjectRepositoryTest {
    companion object {
        @Container
        val postgreSQLContainer = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testDb")
            .withUsername("postgres")
            .withPassword("test")
            .withReuse(true)
    }

    private lateinit var database: Database
    private lateinit var projectRepository: ProjectRepository
    private lateinit var userRepository: UserRepository
    private val testUser1 = User(
        phone = "89036559989",
        username = "owner",
        avatarPath = null
    )
    private val testUser2 = User(
        phone = "89038518685",
        username = "admin",
        avatarPath = null
    )

    @BeforeAll
    fun initialize() {
        database = Database.connect(
            url = postgreSQLContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = "postgres",
            password = "test",
        )
        projectRepository = ProjectRepositoryImpl(database)
        userRepository = UserRepositoryImpl(database)
        transaction {
            SchemaUtils.create(UserTable)
            runBlocking {
                userRepository.insert(testUser1, "testPassword")
                userRepository.insert(testUser2, "testPassword")
            }
        }
    }

    @BeforeEach
    fun setUp(){
        transaction(database) {
            SchemaUtils.drop(ProjectTable, ProjectUsersTable)
            SchemaUtils.create(ProjectTable, ProjectUsersTable)
        }
    }

    @Test
    fun `should receive existing project`() = runTest{
        suspendTransaction {
            //given
            val projectUUID = UUID.randomUUID()
            val testProjectName = "testProject"
            ProjectTable.insert {
                it[ProjectTable.id] = projectUUID
                it[ProjectTable.name] = testProjectName
                it[ProjectTable.membersCount] = 1
            }
            val expectedProject = Project(
                projectID = projectUUID.toString(),
                name = testProjectName,
                membersCount = 1
            )
            //when
            val receivedProject = projectRepository.getProjectById(projectUUID)
            //then
            assertNotNull(receivedProject)
            assertEquals(expectedProject, receivedProject)
        }
    }

    @Test
    fun `should insert project and create record in ProjectUsersTable`() = runTest{
        suspendTransaction {
            //given
            val userPhone = testUser1.phone
            //when
            val createdProject = projectRepository.insertProject(testUser1.phone,"testProject")
            val receivedProject = projectRepository.getProjectById(
                UUID.fromString(createdProject.projectID)
            )

            //then
            assertNotNull(receivedProject)
            assertEquals(createdProject,receivedProject)
            //check record add to user-project table
            val result = transaction(database) {
                ProjectUsersTable.selectAll().singleOrNull()
            }
            assertNotNull(result)
            assertEquals(createdProject.projectID, result[ProjectUsersTable.projectId].toString())
            assertEquals(userPhone, result[ProjectUsersTable.userPhone])
            assertEquals(Role.Owner.toShort(), result[ProjectUsersTable.role])
        }
    }

    @Test
    fun `should receive all user projects`() = runTest{
        suspendTransaction {
            //given
            val userPhone = testUser1.phone
            val projectList = listOf(
                projectRepository.insertProject(testUser1.phone, "testProject"),
                projectRepository.insertProject(testUser1.phone, "testProject2"),
                projectRepository.insertProject(testUser1.phone, "testProject3")
            )
            //when
            val receivedProjects = projectRepository.getAllUserProjects(userPhone)
            //then
            projectList.forEachIndexed { i, project ->
                assertEquals(project, receivedProjects[i].project)
            }
        }
    }

    @Test
    fun `should insert new record in ProjectUsersTable and increment members count in Project table after insert new member`() = runTest{
        suspendTransaction {
            //given
            val inviterPhone = testUser1.phone
            val invitableUserPhone = testUser2.phone
            val project = projectRepository.insertProject(inviterPhone,"testProject")
            //when
            projectRepository.addMemberToProject(invitableUserPhone, project = project, role = Role.Admin)
            //then
            val updatedProject = projectRepository.getProjectById(UUID.fromString(project.projectID))!!
            assertEquals(2,updatedProject.membersCount)
            val result = transaction(database) {
                ProjectUsersTable.selectAll().where{
                    (ProjectUsersTable.userPhone eq invitableUserPhone).and {
                        ProjectUsersTable.projectId eq UUID.fromString(project.projectID)
                    }
                }.singleOrNull()
            }
            assertNotNull(result)
        }
    }

    @Test
    fun `should throw error if user already project member`() = runTest{
        suspendTransaction {
            //given
            val inviterPhone = testUser1.phone
            val project = projectRepository.insertProject(inviterPhone,"testProject")
            //when
            val result = runCatching {
                projectRepository.addMemberToProject(inviterPhone, project = project, role = Role.Admin)
            }
            //then
            assertNotNull(result.exceptionOrNull())
        }
    }
}