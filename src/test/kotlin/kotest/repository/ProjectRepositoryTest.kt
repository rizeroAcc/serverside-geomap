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
import io.kotest.assertions.asClue
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.extensions.testcontainers.perSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldHave
import io.kotest.matchers.shouldNotBe
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
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

    val userRepository : UserRepository by lazy { UserRepositoryImpl(database) }
    val projectRepository : ProjectRepository by lazy { ProjectRepositoryImpl(database) }
    beforeContainer { testCase ->
        if (testCase.parent == null) {
            suspendTransaction(database) {
                SchemaUtils.drop(UserTable, ProjectTable, ProjectUsersTable)
                SchemaUtils.create(UserTable, ProjectTable, ProjectUsersTable)
            }
        }
    }
    beforeEach {
        suspendTransaction(database) {
            ProjectTable.deleteAll()
            ProjectUsersTable.deleteAll()
        }
    }


    context("with created user"){
        val user = createTestUser()
        suspendTransaction(database) { userRepository.insert(user, "testPass") }
        context("insert"){
            test("should insert project"){
                suspendTransaction(database) {
                    val createdProject = projectRepository.insertProject(user.phone,"testProject")
                    ProjectTable.selectAll()
                        .where { ProjectTable.id eq UUID.fromString(createdProject.projectID) }
                        .single().asClue { row ->
                            row[ProjectTable.name] shouldBe createdProject.name
                            row[ProjectTable.membersCount] shouldBe 1.toShort()
                        }
                    //check record add to user-project table
                    ProjectUsersTable
                        .selectAll()
                        .single().asClue { row->
                            row[ProjectUsersTable.projectId] shouldBe UUID.fromString(createdProject.projectID)
                            row[ProjectUsersTable.userPhone] shouldBe user.phone
                            row[ProjectUsersTable.role] shouldBe Role.Owner.toShort()
                        }
                }
            }
        }
        context("get project"){
            test("should get existing project") {
                suspendTransaction(database) {
                    val project = projectRepository.insertProject(user.phone, "testProject")
                    projectRepository.getProjectById(UUID.fromString(project.projectID)) shouldBe project
                }
            }
            test("should get null if project doesn't exist"){
                suspendTransaction(database) {
                    projectRepository.getProjectById(UUID.randomUUID()) shouldBe null
                }
            }
        }
        context("get user projects"){
            test("should receive all user projects"){
                suspendTransaction(database) {
                    val projectList = listOf(
                        projectRepository.insertProject(user.phone, "testProject"),
                        projectRepository.insertProject(user.phone, "testProject2"),
                        projectRepository.insertProject(user.phone, "testProject3")
                    )
                    projectRepository.getAllUserProjects(user.phone).map { it.project } shouldContainAll projectList
                }
            }
            test("should receive empty list if user haven't projects"){
                suspendTransaction(database) {
                    projectRepository.getAllUserProjects(user.phone) shouldHaveSize 0
                }
            }
        }
    }
    context("with 2 existing users"){
        val userOne = createTestUser()
        val userTwo = createTestUser {
            phone = "89038518685"
        }
        suspendTransaction(database) {
            userRepository.insert(userOne, "testPass")
            userRepository.insert(userTwo, "testPass")
        }
        test("should insert new record in ProjectUsersTable and increment members count in Project table after insert new member"){
            suspendTransaction(database) {
                val inviterPhone = userOne.phone
                val invitableUserPhone = userTwo.phone
                val project = projectRepository.insertProject(inviterPhone,"testProject")
                //when
                projectRepository.addMemberToProject(invitableUserPhone, project = project, role = Role.Admin)
                //then
                assertSoftly {
                    projectRepository.getProjectById(UUID.fromString(project.projectID)) shouldNotBeNull {
                        this.membersCount shouldBe 2
                    }
                    transaction(database) {
                        ProjectUsersTable.selectAll().where{
                            (ProjectUsersTable.userPhone eq invitableUserPhone).and {
                                ProjectUsersTable.projectId eq UUID.fromString(project.projectID)
                            }
                        }.singleOrNull() shouldNotBe null
                    }
                }
            }
        }
        test("should throw error if user already project member"){
            suspendTransaction(database) {
                val inviterPhone = userOne.phone
                val invitableUserPhone = userOne.phone
                val project = projectRepository.insertProject(inviterPhone,"testProject")
                //when
                shouldThrow<ExposedSQLException> {
                    projectRepository.addMemberToProject(invitableUserPhone, project = project, role = Role.Worker)
                }
            }
        }
    }
})