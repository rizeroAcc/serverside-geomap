package com.mapprjct.dao

import com.mapprjct.builders.createTestUser
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
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
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
    }

    @BeforeEach
    fun setUp(){
        transaction(database) {
            SchemaUtils.drop(UserTable,ProjectTable, ProjectUsersTable)
            SchemaUtils.create(UserTable,ProjectTable, ProjectUsersTable)
        }
    }

    suspend fun createAndRegisterNewUser(phone : String) : User {
        val user = createTestUser { this.phone = phone }
        userRepository.insert(user = user, password = UUID.randomUUID().toString())
        return user
    }

    @Test
    fun `should insert project and create record in ProjectUsersTable`() = runTest{
        suspendTransaction {
            val user = createAndRegisterNewUser(phone = "89036559989")
            //when
            val createdProject = projectRepository.insertProject(user.phone,"testProject")
            //then
            //check project created
            ProjectTable.selectAll()
                .where { ProjectTable.id eq UUID.fromString(createdProject.projectID) }
                .single().let { row ->
                    assertThat(row).satisfies(
                        { it[ProjectTable.name] == createdProject.name },
                        { it[ProjectTable.membersCount] == 1.toShort() }
                    )
                }
            //check record add to user-project table
            ProjectUsersTable
                .selectAll()
                .single().let { row ->
                    assertThat(row).satisfies(
                        { it[ProjectUsersTable.projectId] == UUID.fromString(createdProject.projectID) },
                        { it[ProjectUsersTable.userPhone] == user.phone },
                        { it[ProjectUsersTable.role] == Role.Owner.toShort() }
                    )
                }
        }
    }

    @Test
    fun `should receive existing project`() = runTest{
        suspendTransaction {
            //given
            val user = createAndRegisterNewUser("89036559989")
            val createdProject = projectRepository.insertProject(user.phone,"testProject")
            //when
            val receivedProject = projectRepository.getProjectById(UUID.fromString(createdProject.projectID))
            //then
            assertThat(receivedProject)
                .isNotNull()
                .isEqualTo(createdProject)
        }
    }



    @Test
    fun `should receive all user projects`() = runTest{
        suspendTransaction {
            //given
            val user = createAndRegisterNewUser("89036559989")
            val projectList = listOf(
                projectRepository.insertProject(user.phone, "testProject"),
                projectRepository.insertProject(user.phone, "testProject2"),
                projectRepository.insertProject(user.phone, "testProject3")
            )
            //when
            val receivedProjects = projectRepository.getAllUserProjects(user.phone).map { it.project }
            //then
            assertThat(receivedProjects)
                .hasSize(3)
                .containsAll(projectList)
        }
    }

    @Test
    fun `should insert new record in ProjectUsersTable and increment members count in Project table after insert new member`() = runTest{
        suspendTransaction {
            //given
            val user1 = createAndRegisterNewUser("89036559989")
            val user2 = createAndRegisterNewUser("89038518685")
            val inviterPhone = user1.phone
            val invitableUserPhone = user2.phone
            val project = projectRepository.insertProject(inviterPhone,"testProject")
            //when
            projectRepository.addMemberToProject(invitableUserPhone, project = project, role = Role.Admin)
            //then
            val updatedProject = projectRepository.getProjectById(UUID.fromString(project.projectID))!!
            assertThat(updatedProject)
                .extracting { it.membersCount }
                .isEqualTo(2)
            val result = transaction(database) {
                ProjectUsersTable.selectAll().where{
                    (ProjectUsersTable.userPhone eq invitableUserPhone).and {
                        ProjectUsersTable.projectId eq UUID.fromString(project.projectID)
                    }
                }.singleOrNull()
            }
            assertThat(result)
                .isNotNull()
        }
    }

    @Test
    fun `should throw error if user already project member`() = runTest{
        suspendTransaction {
            val user = createAndRegisterNewUser("89036559989")
            //given
            val inviterPhone = user.phone
            val project = projectRepository.insertProject(inviterPhone,"testProject")
            //when
            val result = runCatching {
                projectRepository.addMemberToProject(inviterPhone, project = project, role = Role.Admin)
            }
            //then
            assertThat(result.isFailure)
        }
    }
}