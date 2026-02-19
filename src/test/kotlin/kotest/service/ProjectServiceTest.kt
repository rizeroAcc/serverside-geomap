package com.mapprjct.kotest.service

import com.mapprjct.AppConfig
import com.mapprjct.builders.createTestUser
import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.tables.InviteCodeTable
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.database.tables.UserTable
import com.mapprjct.di.repositoryModule
import com.mapprjct.di.serviceModule
import com.mapprjct.di.storageModule
import com.mapprjct.model.Invitation
import com.mapprjct.model.dto.Project
import com.mapprjct.model.dto.User
import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.service.ProjectService
import com.mapprjct.service.UserService
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.koin.KoinExtension
import io.kotest.koin.KoinLifecycleMode
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.testcontainers.containers.PostgreSQLContainer
import java.util.UUID
import kotlin.getValue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ProjectServiceTest : KoinTest, FunSpec() {
//    init {
//        val postgres = PostgreSQLContainer("postgres:latest")
//            .withDatabaseName("testdb")
//            .withUsername("postgres")
//            .withPassword("postgres")
//            .withReuse(true)
//
//        val database by lazy {
//            Database.connect(
//                url = postgres.jdbcUrl,
//                driver = "org.postgresql.Driver",
//                user = postgres.username,
//                password = postgres.password
//            )
//        }
//
//        install(TestContainerSpecExtension(postgres))
//        extension(
//            KoinExtension(
//                modules = listOf(
//                    module {
//                        single { database }
//                        single { AppConfig.Test }
//                    },
//                    storageModule,
//                    repositoryModule,
//                    serviceModule
//                ),
//                mode = KoinLifecycleMode.Root
//            )
//        )
//        // Подключаемся один раз к контейнеру
//
//        val invitationRepository : InvitationRepository by inject()
//        val userService:UserService by inject()
//        val projectService : ProjectService by inject()
//
//        beforeSpec {
//            suspendTransaction(database) {
//                SchemaUtils.create(UserTable, ProjectTable, ProjectUsersTable, InviteCodeTable)
//            }
//        }
//        beforeContainer {
//            suspendTransaction(database) {
//                UserTable.deleteAll()
//            }
//        }
//        beforeTest {
//            suspendTransaction (database){
//                ProjectTable.deleteAll()
//                InviteCodeTable.deleteAll()
//            }
//        }
//
//        suspend fun createAndSaveInvitation(owner: User, project: Project): Invitation = suspendTransaction {
//            val invitation = Invitation(
//                inviterPhone = owner.phone,
//                inviteCode = UUID.randomUUID(),
//                projectID = UUID.fromString(project.projectID),
//                expireAt = Clock.System.now().toEpochMilliseconds() + 86_400_000, // 24 часа
//                role = Role.Worker
//            )
//            invitationRepository.insertInvitation(invitation)
//            invitation
//        }
//
//        context("create project") {
//            val user = createTestUser {  }
//            val credentials = UserCredentials(user.phone, "testPass")
//            userService.createUser(credentials, user.username)
//            test("should create a new project") {
//                val projectName = "testProject"
//                val createdProject = shouldNotThrowAny {
//                    projectService.createProject(
//                        user.phone, projectName
//                    ).getOrThrow()
//                }
//                //then
//                suspendTransaction(database) {
//                    ProjectTable.selectAll().where {
//                        ProjectTable.id eq UUID.fromString(createdProject.projectID)
//                    }.single().asClue {
//                        it[ProjectTable.id].toString() shouldBe createdProject.projectID
//                        it[ProjectTable.name] shouldBe createdProject.name
//                        it[ProjectTable.membersCount].toInt() shouldBe createdProject.membersCount
//                    }
//                }
//            }
//            test("should return UserNotFoundException when user does not exist"){
//                val wrongPhone = "89036559999"
//                shouldThrow<UserDMLExceptions.UserNotFoundException> {
//                    projectService.createProject(wrongPhone, "test").getOrThrow()
//                }
//            }
//        }
//        context("get project") {
//            val user = createTestUser {  }
//            val credentials = UserCredentials(user.phone, "testPass")
//            userService.createUser(credentials, user.username)
//            test("should get existing project"){
//                val projectName = "testProject"
//                val project = projectService.createProject(
//                    creatorPhone = user.phone,
//                    projectName = projectName
//                ).getOrThrow()
//                projectService.getProject(projectID = project.projectID).getOrThrow() shouldBe project
//            }
//            test("should return IllegalArgumentException when project id invalid"){
//                val invalidProjectId = "invalidProjectId"
//                shouldThrow<IllegalArgumentException>{
//                    projectService.getProject(invalidProjectId).getOrThrow()
//                }
//            }
//            test("should return ProjectNotFoundException when project doesn't exists"){
//                val projectID = UUID.randomUUID().toString()
//                shouldThrow<ProjectDMLException.ProjectNotFoundException>{
//                    projectService.getProject(projectID = projectID).getOrThrow()
//                }
//            }
//        }
//        context("get all user projects"){
//            val user = createTestUser {  }
//            val credentials = UserCredentials(user.phone, "testPass")
//            userService.createUser(credentials, user.username)
//            test("should get all user projects"){
//                val userProjects = listOf(
//                    projectService.createProject(user.phone,"test1").getOrThrow(),
//                    projectService.createProject(user.phone,"test2").getOrThrow(),
//                    projectService.createProject(user.phone,"test3").getOrThrow()
//                )
//                projectService.getAllUserProjects(user.phone)
//                    .getOrThrow()
//                    .map{it.project} shouldContainAll userProjects
//            }
//            test("should return UserNotFoundException if user does not exist"){
//                val wrongPhone = "89036559999"
//                shouldThrow<UserDMLExceptions.UserNotFoundException> {
//                    projectService.getAllUserProjects(wrongPhone).getOrThrow()
//                }
//            }
//        }
//        context("join project"){
//            val inviterUser = createTestUser {
//                phone = "89036559989"
//            }
//            val inviterCredentials = UserCredentials(inviterUser.phone, "testPass")
//            userService.createUser(inviterCredentials, inviterUser.username)
//
//            val userWhoInvited = createTestUser {
//                phone = "89038518685"
//            }
//            val invitedCredentials = UserCredentials(userWhoInvited.phone, "testPass")
//            userService.createUser(invitedCredentials, userWhoInvited.username)
//
//            test("should add user to project by invitation and delete invitation"){
//                var project = projectService.createProject(inviterUser.phone, "test").getOrThrow()
//                val invitation = createAndSaveInvitation(inviterUser, project)
//                //when
//                project = shouldNotThrowAny {
//                    projectService.joinProject(
//                        userPhone = userWhoInvited.phone,
//                        invitationCode = invitation.inviteCode.toString()
//                    ).getOrThrow()
//                }
//                projectService.getAllUserProjects(userWhoInvited.phone)
//                    .getOrThrow()
//                    .map {it.project} shouldContain project
//                suspendTransaction(database) {
//                    invitationRepository.getInvitation(invitation.inviteCode) shouldBe null
//                }
//            }
//            test("should return UserAlreadyProjectMember when user already stay in project"){
//                var project = projectService.createProject(inviterUser.phone, "test").getOrThrow()
//                val invitation = createAndSaveInvitation(inviterUser, project)
//                shouldThrow<ProjectValidationException.UserAlreadyProjectMember> {
//                    projectService.joinProject(
//                        userPhone = inviterUser.phone,
//                        invitationCode = invitation.inviteCode.toString()
//                    ).getOrThrow()
//                }
//            }
//            test("should return InvitationNotFoundException if invitation doesn't exist"){
//                shouldThrow<InvitationDMLExceptions.InvitationNotFoundException> {
//                    projectService.joinProject(userWhoInvited.phone, UUID.randomUUID().toString()).getOrThrow()
//                }
//            }
//        }
//    }
}