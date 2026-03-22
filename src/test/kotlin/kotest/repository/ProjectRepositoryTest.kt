package com.mapprjct.kotest.repository

import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.repositoryImpl.ProjectRepositoryImpl
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.database.tables.UserTable
import com.mapprjct.model.datatype.Role
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.dto.ProjectDTO
import com.mapprjct.model.dto.UnregisteredProjectDTO
import com.mapprjct.utils.toStringUUID
import com.mapprjct.utils.toUUID
import com.mapprjct.withRegisteredUser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.postgresql.util.PSQLState
import org.testcontainers.containers.PostgreSQLContainer
import java.util.*

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



    val projectRepository: ProjectRepository by lazy { ProjectRepositoryImpl(database) }

    beforeSpec {
        transaction(database) { SchemaUtils.create(UserTable) }
    }
    beforeContainer {
        suspendTransaction(database) {
            UserTable.deleteAll()
        }
    }
    afterContainer {
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

        withData(listOf(null, UUID.randomUUID().toStringUUID())) { oldProjectId ->
            test("Should insert project and create record in ProjectUsersTable. Should return old project id if it passed") {
                withRegisteredUser { userPhone ->
                    suspendTransaction {
                        val createdProject = projectRepository.insert(
                            userPhone,
                            UnregisteredProjectDTO(name = "testProject", oldID = oldProjectId)
                        )
                        //check project created
                        ProjectTable.selectAll()
                            .where { ProjectTable.id eq createdProject.projectDTO.projectID.toUUID() }
                            .single().let { row ->
                                row[ProjectTable.name] shouldBe createdProject.projectDTO.name
                                row[ProjectTable.membersCount] shouldBe 1.toShort()
                            }
                        //check record add to user-project table
                        ProjectUsersTable
                            .selectAll()
                            .single().let { row ->
                                row[ProjectUsersTable.projectId] shouldBe createdProject.projectDTO.projectID.toUUID()
                                row[ProjectUsersTable.role] shouldBe Role.Owner.toShort()
                                row[ProjectUsersTable.userPhone] shouldBe userPhone.normalizeAsRussiaPhone()
                            }
                        createdProject.oldID shouldBe oldProjectId
                    }
                }
            }
        }
        test("should throw error if user phone does not exist") {
            withRegisteredUser { userPhone ->
                val unregisteredUserPhone = RussiaPhoneNumber(userPhone.normalizeAsRussiaPhone().replace("3","4"))
                val ex = shouldThrow<ExposedSQLException> {
                    suspendTransaction {
                        projectRepository.insert(unregisteredUserPhone, UnregisteredProjectDTO(name = "testProject"))
                    }
                }
                ex.sqlState shouldBe PSQLState.FOREIGN_KEY_VIOLATION.state
            }
        }
    }
    context("get project") {
        withRegisteredUser { userPhone ->
            test("should receive existing project") {
                suspendTransaction {
                    val createdProject = projectRepository.insert(
                        userPhone,
                        UnregisteredProjectDTO(name = "testProject")
                    )
                    projectRepository.getProjectById(createdProject.projectDTO.projectID.toUUID()) shouldBe createdProject.projectDTO
                }
            }
            test("should receive null if project does not exist") {
                suspendTransaction {
                    projectRepository.getProjectById(UUID.randomUUID()) shouldBe null
                }
            }
            test("should receive all user projects") {
                suspendTransaction {
                    val projectList = listOf(
                        projectRepository.insert(userPhone,UnregisteredProjectDTO(name = "testProject")),
                        projectRepository.insert(userPhone,UnregisteredProjectDTO(name = "testProject2")),
                        projectRepository.insert(userPhone,UnregisteredProjectDTO(name = "testProject3"))
                    )
                    projectRepository
                        .findAllUserProjects(userPhone)
                        .map {it.projectDTO} shouldContainAll projectList.map { it.projectDTO }
                }
            }
            test("should receive empty list if user hasn't projects") {
                suspendTransaction {
                    projectRepository.findAllUserProjects(userPhone) shouldHaveSize 0
                }
            }
        }
    }
    context("add member to project") {

        withRegisteredUser { inviterUserPhone ->
            withRegisteredUser(phone = RussiaPhoneNumber("89038518685")) { invitableUserPhone ->
                test("should insert new record in ProjectUsersTable and increment members count in Project table") {
                    suspendTransaction {
                        val project = projectRepository.insert(inviterUserPhone, UnregisteredProjectDTO(name = "testProject")).projectDTO
                        projectRepository.addMemberToProject(userPhone = invitableUserPhone,projectDTO = project,role = Role.Admin)

                        projectRepository.getProjectById(project.projectID.toUUID())
                            .shouldNotBeNull()
                            .membersCount shouldBe 2
                        ProjectUsersTable.selectAll().where {
                            (ProjectUsersTable.userPhone eq invitableUserPhone.normalizeAsRussiaPhone())
                                .and { ProjectUsersTable.projectId eq project.projectID.toUUID() }
                        }.singleOrNull() shouldNotBe null
                    }
                }
                test("should throw foreign key exception if invitable user phone not registered") {
                    suspendTransaction {
                        val unregisteredUserPhone = RussiaPhoneNumber("89038518688")
                        val project = projectRepository.insert(inviterUserPhone, UnregisteredProjectDTO(name = "testProject")).projectDTO
                        shouldThrow<ExposedSQLException> {
                            suspendTransaction {
                                projectRepository.addMemberToProject(userPhone = unregisteredUserPhone,projectDTO = project,role = Role.Admin)
                            }
                        }.sqlState shouldBe PSQLState.FOREIGN_KEY_VIOLATION.state
                    }
                }
                test("should throw foreign key exception if project not existing") {
                    val unregisteredProject = ProjectDTO(UUID.randomUUID().toStringUUID(),"name",1)
                    shouldThrow<ExposedSQLException> {
                        suspendTransaction {
                            projectRepository.addMemberToProject(userPhone = invitableUserPhone,projectDTO = unregisteredProject,role = Role.Admin)
                        }
                    }.sqlState shouldBe PSQLState.FOREIGN_KEY_VIOLATION.state
                }
            }
        }
    }
}
)