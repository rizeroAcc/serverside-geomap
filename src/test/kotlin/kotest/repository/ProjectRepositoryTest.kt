package com.mapprjct.kotest.repository

import com.mapprjct.builders.createTestUser
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.repositoryImpl.ProjectRepositoryImpl
import com.mapprjct.database.repositoryImpl.UserRepositoryImpl
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.database.tables.UserTable
import com.mapprjct.model.datatype.Password
import com.mapprjct.model.datatype.Role
import com.mapprjct.utils.toStringUUID
import com.mapprjct.utils.toUUID
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
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

    val userRepository: UserRepository by lazy { UserRepositoryImpl(database) }
    val projectRepository: ProjectRepository by lazy { ProjectRepositoryImpl(database) }
    beforeSpec {
        transaction(database) { SchemaUtils.create(UserTable) }
    }
    beforeContainer {
        suspendTransaction(database) {
            UserTable.deleteAll()
        }
    }
    beforeEach {
        transaction(database) {
            SchemaUtils.drop(ProjectTable, ProjectUsersTable)
            SchemaUtils.create(ProjectTable, ProjectUsersTable)
        }
    }
    context("insert") {
        val testUser = createTestUser()
        val testUserPassword = Password("testPassword")
        suspendTransaction(database) {
            userRepository.insert(testUser,testUserPassword)
        }
        test("should insert project and create record in ProjectUsersTable") {
            suspendTransaction {
                val createdProject = projectRepository.insertProject(
                    testUser.phone,
                    "testProject"
                )
                //check project created
                ProjectTable.selectAll()
                    .where { ProjectTable.id eq createdProject.projectID.toUUID() }
                    .single().let {row->
                        row[ProjectTable.name] shouldBe createdProject.name
                        row[ProjectTable.membersCount] shouldBe 1.toShort()
                    }
                //check record add to user-project table
                ProjectUsersTable
                    .selectAll()
                    .single().let {row->
                        row[ProjectUsersTable.projectId] shouldBe createdProject.projectID.toUUID()
                        row[ProjectUsersTable.role] shouldBe Role.Owner.toShort()
                        row[ProjectUsersTable.userPhone] shouldBe testUser.phone.value
                    }
            }
        }
    }
    context("get project") {
        val testUser = createTestUser()
        val testUserPassword = Password("testPassword")
        suspendTransaction(database) {
            userRepository.insert(testUser,testUserPassword)
        }
        test("should receive existing project") {
            suspendTransaction {
                val createdProject = projectRepository.insertProject(
                    testUser.phone,
                    "testProject"
                )
                projectRepository.getProjectById(createdProject.projectID.toUUID()) shouldBe createdProject
            }
        }
        test("should receive all user projects") {
            suspendTransaction {
                val projectList = listOf(
                    projectRepository.insertProject(testUser.phone,"testProject"),
                    projectRepository.insertProject(testUser.phone,"testProject2"),
                    projectRepository.insertProject(testUser.phone,"testProject3")
                )
                projectRepository
                    .getAllUserProjects(testUser.phone)
                    .map {it.project} shouldContainAll projectList
            }
        }
    }
    context("add member to project") {

        val testUser = createTestUser()
        val testUserPassword = Password("testPassword")
        suspendTransaction(database) {
            userRepository.insert(testUser,testUserPassword)
        }

        val invitedUser = createTestUser {
            phone = "89038518685"
        }
        suspendTransaction {
            userRepository.insert(invitedUser, Password("testPass"))
        }

        test("should insert new record in ProjectUsersTable and increment members count in Project table") {
            suspendTransaction {
                val inviterPhone = testUser.phone
                val invitableUserPhone = invitedUser.phone
                val project = projectRepository.insertProject(inviterPhone,"testProject")
                //when
                projectRepository.addMemberToProject(userPhone = invitableUserPhone,project = project,role = Role.Admin)

                projectRepository.getProjectById(project.projectID.toUUID())
                    .shouldNotBeNull()
                    .membersCount shouldBe 2
                ProjectUsersTable.selectAll().where {
                    (ProjectUsersTable.userPhone eq invitableUserPhone.normalizeAsRussiaPhone())
                        .and { ProjectUsersTable.projectId eq project.projectID.toUUID() }
                }.singleOrNull() shouldNotBe null
            }
        }
    }
}
)