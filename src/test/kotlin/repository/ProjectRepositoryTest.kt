package com.mapprjct.dao

import com.mapprjct.database.dao.ProjectRepository
import com.mapprjct.database.dao.UserRepository
import com.mapprjct.database.daoimpl.ProjectRepositoryImpl
import com.mapprjct.database.daoimpl.UserRepositoryImpl
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.database.tables.UserTable
import com.mapprjct.dto.Project
import com.mapprjct.dto.Role
import com.mapprjct.dto.User
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
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
    @AfterEach
    fun tearDown(){
        transaction { SchemaUtils.drop(ProjectTable, ProjectUsersTable) }
    }

    @Test
    fun `should receive existing project`() = runTest{
        val projectUUID = UUID.randomUUID()
        val testProjectName = "testProject"
        transaction(database) {
            ProjectTable.insert {
                it[ProjectTable.id] = projectUUID
                it[ProjectTable.name] = testProjectName
                it[ProjectTable.membersCount] = 1
            }
        }

        val receivedProject = projectRepository.getProjectById(projectUUID)
        val expectedProject = Project(
            projectID = projectUUID.toString(),
            name = testProjectName,
            membersCount = 1
        )

        assertNotNull(receivedProject)
        assertEquals(expectedProject, receivedProject)

    }

    @Test
    fun `should insert project and create record in ProjectUsersTable`() = runTest{
        val userPhone = testUser1.phone
        val createdProject = projectRepository.insertProject(testUser1.phone,"testProject")

        //check project saved
        val receivedProject = projectRepository.getProjectById(
            UUID.fromString(createdProject.projectID)
        )
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

    @Test
    fun `should receive all user projects`() = runTest{
        val userPhone = testUser1.phone
        val projectList = listOf(
            projectRepository.insertProject(testUser1.phone, "testProject"),
            projectRepository.insertProject(testUser1.phone, "testProject2"),
            projectRepository.insertProject(testUser1.phone, "testProject3")
        )

        val receivedProjects = projectRepository.getAllUserProjects(userPhone)

        projectList.forEachIndexed { i, project ->
            assertEquals(project, receivedProjects[i].project)
        }
    }

    @Test
    fun `should insert new record in ProjectUsersTable and increment members count in Project table after insert new member`() = runTest{
        val inviterPhone = testUser1.phone
        val invitableUserPhone = testUser2.phone
        val project = projectRepository.insertProject(inviterPhone,"testProject")

        projectRepository.addMemberToProject(invitableUserPhone, project = project, role = Role.Admin)

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

    @Test
    fun `should throw error if user already project member`() = runTest{
        val inviterPhone = testUser1.phone
        val project = projectRepository.insertProject(inviterPhone,"testProject")

        val result = runCatching {
            projectRepository.addMemberToProject(inviterPhone, project = project, role = Role.Admin)
        }
        assertNotNull(result.exceptionOrNull())
    }
}