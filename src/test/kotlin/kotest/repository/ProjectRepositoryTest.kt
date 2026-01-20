package com.mapprjct.kotest.repository

import com.mapprjct.builders.createTestUser
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.repositoryImpl.ProjectRepositoryImpl
import com.mapprjct.database.repositoryImpl.UserRepositoryImpl
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.database.tables.UserTable
import com.mapprjct.model.dto.Role
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotBe
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer
import java.util.UUID

class ProjectRepositoryTest : FunSpec({
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

    val userRepository: UserRepository by lazy { UserRepositoryImpl(database) }
    val projectRepository: ProjectRepository by lazy { ProjectRepositoryImpl(database) }
    beforeSpec {
        transaction(database) { SchemaUtils.create(UserTable) }
    }
    beforeEach {
        transaction(database) {
            SchemaUtils.drop(ProjectTable, ProjectUsersTable)
            SchemaUtils.create(ProjectTable, ProjectUsersTable)
        }
    }
    context("With created user") {
        val testUser = createTestUser()
        val testUserPassword = "testPassword"
        suspendTransaction(database) { userRepository.insert(testUser, testUserPassword) }
        context("insert") {
            test("should insert project and create record in ProjectUsersTable") {
                suspendTransaction {
                    val createdProject = projectRepository.insertProject(
                        testUser.phone,
                        "testProject"
                    )
                    val projectUUID = UUID.fromString(createdProject.projectID)
                    //check project created
                    ProjectTable.selectAll()
                        .where { ProjectTable.id eq UUID.fromString(createdProject.projectID) }
                        .single().let { row ->
                            row[ProjectTable.name] shouldBe createdProject.name
                            row[ProjectTable.membersCount] shouldBe 1.toShort()
                        }
                    //check record add to user-project table
                    ProjectUsersTable
                        .selectAll()
                        .single().let { row ->
                            row[ProjectUsersTable.projectId] shouldBe projectUUID
                            row[ProjectUsersTable.role] shouldBe Role.Owner.toShort()
                            row[ProjectUsersTable.userPhone] shouldBe testUser.phone
                        }
                }
            }
        }
        context("get project"){
            test("should receive existing project"){
                suspendTransaction {
                    val createdProject = projectRepository.insertProject(
                        testUser.phone,
                        "testProject"
                    )
                    projectRepository.getProjectById(
                        UUID.fromString(createdProject.projectID)
                    ) shouldBe createdProject
                }
            }
            test("should receive all user projects"){
                suspendTransaction {
                    val projectList = listOf(
                        projectRepository.insertProject(testUser.phone, "testProject"),
                        projectRepository.insertProject(testUser.phone, "testProject2"),
                        projectRepository.insertProject(testUser.phone, "testProject3")
                    )
                    projectRepository
                        .getAllUserProjects(testUser.phone)
                        .map { it.project } shouldContainAll projectList
                }
            }
        }
        context("add member to project"){
            val invitedUser = createTestUser{
                username = "89038518685"
            }
            suspendTransaction { userRepository.insert(invitedUser, "testPass") }
            test("should insert new record in ProjectUsersTable and increment members count in Project table"){
                suspendTransaction {
                    val inviterPhone = testUser.phone
                    val invitableUserPhone = invitedUser.phone
                    val project = projectRepository.insertProject(inviterPhone,"testProject")
                    //when
                    projectRepository.addMemberToProject(invitableUserPhone, project = project, role = Role.Admin)
                    //then
                    projectRepository.getProjectById(UUID.fromString(project.projectID))
                        .shouldNotBeNull().membersCount shouldBe 2
                    transaction(database) {
                        ProjectUsersTable.selectAll().where{
                            (ProjectUsersTable.userPhone eq invitableUserPhone).and {
                                ProjectUsersTable.projectId eq UUID.fromString(project.projectID)
                            }
                        }.singleOrNull()
                    } shouldNotBe null
                }
            }
        }
    }
}
)