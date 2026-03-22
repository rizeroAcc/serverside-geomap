package com.mapprjct.kotest.service

import com.mapprjct.builders.createInvitation
import com.mapprjct.builders.createTestProject
import com.mapprjct.builders.createTestUser
import com.mapprjct.com.mapprjct.utils.BypassTransactionProvider
import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.exceptions.domain.project.FindAllUserProjectsException
import com.mapprjct.exceptions.domain.project.FindProjectException
import com.mapprjct.exceptions.domain.project.JoinProjectException
import com.mapprjct.exceptions.domain.project.ProjectRegistrationError
import com.mapprjct.model.datatype.Role
import com.mapprjct.model.dto.ProjectDTO
import com.mapprjct.model.dto.ProjectMembershipDTO
import com.mapprjct.model.dto.ProjectRegistrationResultDTO
import com.mapprjct.model.dto.UnregisteredProjectDTO
import com.mapprjct.service.ProjectService
import com.mapprjct.utils.toStringUUID
import com.mapprjct.utils.toUUID
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.koin.test.KoinTest
import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ProjectServiceTest : KoinTest, FunSpec() {

    val invitationRepository : InvitationRepository = mockk()
    val userRepository: UserRepository = mockk()
    val projectRepository : ProjectRepository = mockk()

    val projectService : ProjectService = ProjectService(
        transactionProvider = BypassTransactionProvider(),
        userRepository = userRepository,
        projectRepository = projectRepository,
        invitationRepository = invitationRepository
    )

    val userDTO = createTestUser {  }
    init {
        beforeTest {
            clearAllMocks(answers = true, recordedCalls = true)
        }


        context("register project") {
            val projectName = "testProject"
            val unregisteredProjectWithoutOldID = UnregisteredProjectDTO(name = projectName, oldID = null)
            test("should create a new project") {

                val createdProjectID = UUID.randomUUID().toStringUUID()
                coEvery { userRepository.findUser(any()) } returns userDTO
                coEvery { projectRepository.insert(any(),any()) } answers {
                    ProjectRegistrationResultDTO(
                        ProjectDTO(
                            projectID = createdProjectID,
                            secondArg<UnregisteredProjectDTO>().name,
                            membersCount = 1
                        ),
                        oldID = secondArg<UnregisteredProjectDTO>().oldID
                    )
                }
                val clientProjectID = UUID.randomUUID().toStringUUID()
                val unregisteredProjectWithOldID = unregisteredProjectWithoutOldID.copy(oldID = clientProjectID)

                projectService.registerProject(userDTO.phone, unregisteredProjectWithOldID) shouldBeRight ProjectRegistrationResultDTO(
                    projectDTO = ProjectDTO(
                        projectID = createdProjectID,
                        name = projectName,
                        membersCount = 1,
                    ),
                    oldID = unregisteredProjectWithOldID.oldID
                )
                coVerify(exactly = 1) {
                    userRepository.findUser(any())
                    projectRepository.insert(
                        creatorPhone = eq(userDTO.phone),
                        project = eq(unregisteredProjectWithOldID)
                    )
                }
            }
            test("should return UserNotFoundException when user does not exist"){

                coEvery { userRepository.findUser(any()) } returns null
                projectService.registerProject(userDTO.phone,unregisteredProjectWithoutOldID) shouldBeLeft ProjectRegistrationError.UserNotFound(userDTO.phone.value)
                coVerify(exactly = 1) { userRepository.findUser(any()) }
                coVerify(exactly = 0) { projectRepository.insert(any(),any()) }
            }
            test("should return InvalidProjectName if project name is blank"){
                val unregisteredProjectDTO = UnregisteredProjectDTO(name = "    ", oldID = null)
                projectService.registerProject(userDTO.phone,unregisteredProjectDTO) shouldBeLeft ProjectRegistrationError.BlankProjectName
                coVerify(exactly = 0) { projectRepository.insert(any(),any()) }
            }
            test("should return Database error on ExposedSQLException"){
                val databaseException = ExposedSQLException(null,emptyList(),mockk())
                coEvery { userRepository.findUser(any()) } returns userDTO
                coEvery { projectRepository.insert(any(),any()) } throws databaseException
                projectService.registerProject(userDTO.phone,unregisteredProjectWithoutOldID) shouldBeLeft ProjectRegistrationError.Database(databaseException)
                coVerify(exactly = 1) { userRepository.findUser(any()) }
                coVerify(exactly = 1) { projectRepository.insert(any(),any()) }
            }
            test("should return Unknown error on unknown throwable"){
                val exception = Throwable()
                coEvery { userRepository.findUser(any()) } throws exception
                projectService.registerProject(userDTO.phone,unregisteredProjectWithoutOldID)
                coVerify(exactly = 1) { userRepository.findUser(any()) }
                coVerify(exactly = 0) { projectRepository.insert(any(),any()) }
            }
        }
        context("register project list"){
            val unregisteredProjectsListWithClientID = listOf("p1","p2","p3","p4","p5","p6").map {
                pName-> UnregisteredProjectDTO(name = pName, oldID = UUID.randomUUID().toStringUUID())
            }
            test("should return create all projects") {
                val insertedProjectsID = buildList {
                    repeat(unregisteredProjectsListWithClientID.size) {
                        add(UUID.randomUUID().toStringUUID())
                    }
                }
                coEvery { projectRepository.insertAll(any(),any()) } answers {
                    buildList {
                        repeat(secondArg<List<UnregisteredProjectDTO>>().size){ index->
                            add(ProjectRegistrationResultDTO(
                                projectDTO = ProjectDTO(
                                    projectID = insertedProjectsID[index],
                                    name = secondArg<List<UnregisteredProjectDTO>>()[index].name,
                                    membersCount = 1,
                                ),
                                oldID = secondArg<List<UnregisteredProjectDTO>>()[index].oldID,
                            ))
                        }
                    }
                }
                coEvery { userRepository.findUser(any()) } returns userDTO
                projectService.registerProjectList(userDTO.phone,unregisteredProjectsListWithClientID) shouldBeRight buildList {
                    repeat(unregisteredProjectsListWithClientID.size) { index->
                        add(ProjectRegistrationResultDTO(
                            projectDTO = ProjectDTO(
                                projectID = insertedProjectsID[index],
                                name = unregisteredProjectsListWithClientID[index].name,
                                membersCount = 1
                            ),
                            oldID = unregisteredProjectsListWithClientID[index].oldID,
                        ))
                    }
                }
                coVerify(exactly = 1) { userRepository.findUser(any()) }
                coVerify(exactly = 1) { projectRepository.insertAll(eq(userDTO.phone),eq(unregisteredProjectsListWithClientID)) }
            }
            test("should return UserNotFoundException when user does not exist"){
                coEvery { userRepository.findUser(any()) } returns null
                projectService.registerProjectList(userDTO.phone,unregisteredProjectsListWithClientID) shouldBeLeft ProjectRegistrationError.UserNotFound(userDTO.phone.value)
                coVerify(exactly = 0) { projectRepository.insertAll(any(),any()) }
            }
            test("should return InvalidProjectName if at least one project name is blank"){
                val projectListWithBlankName = unregisteredProjectsListWithClientID + listOf(UnregisteredProjectDTO(
                    name = " " ,
                    oldID = UUID.randomUUID().toStringUUID()
                ))
                projectService.registerProjectList(userDTO.phone,projectListWithBlankName) shouldBeLeft ProjectRegistrationError.BlankProjectName
                coEvery { userRepository.findUser(any()) } returns userDTO
                coVerify(exactly = 0) { projectRepository.insertAll(any(),any()) }
            }
            test("should return Database error on ExposedSQLException"){
                val databaseException = ExposedSQLException(null,emptyList(),mockk())
                coEvery { projectRepository.insertAll(any(),any()) } throws databaseException
                coEvery { userRepository.findUser(any()) } returns userDTO
                projectService.registerProjectList(userDTO.phone,unregisteredProjectsListWithClientID) shouldBeLeft ProjectRegistrationError.Database(databaseException)
                coVerify(exactly = 1) { userRepository.findUser(any()) }
                coVerify(exactly = 1) { projectRepository.insertAll(any(),any()) }
            }
            test("should return Unknown error on unknown throwable"){
                val exception = Throwable()
                coEvery { userRepository.findUser(any()) } throws exception
                projectService.registerProjectList(userDTO.phone,unregisteredProjectsListWithClientID)
                coVerify(exactly = 0) { projectRepository.insertAll(any(),any()) }
                coVerify(exactly = 1) { userRepository.findUser(any()) }
            }
        }
        context("get project") {
            val project = createTestProject { name = "test project" }
            val projectID = project.projectID
            test("should get existing project"){

                coEvery { projectRepository.getProjectById(any()) } returns project
                projectService.getProject(projectID) shouldBeRight project
                coVerify(exactly = 1) { projectRepository.getProjectById(eq(projectID.toUUID()))}
            }
            test("should return NotFound when project doesn't exists"){
                coEvery { projectRepository.getProjectById(any()) } returns null
                projectService.getProject(projectID) shouldBeLeft FindProjectException.NotFound(projectID.value)
                coVerify(exactly = 1) { projectRepository.getProjectById(eq(projectID.toUUID()))}
            }
            test("should return Database error on ExposedSQLException"){
                val databaseException = ExposedSQLException(null,emptyList(),mockk())
                coEvery { projectRepository.getProjectById(any()) } throws databaseException
                projectService.getProject(projectID) shouldBeLeft FindProjectException.Database(databaseException)
                coVerify(exactly = 1) { projectRepository.getProjectById(any()) }
            }
            test("should return Unknown error on any throwable"){
                val exception = Throwable()
                coEvery { projectRepository.getProjectById(any()) } throws exception
                projectService.getProject(projectID) shouldBeLeft FindProjectException.Unexpected(exception)
            }
        }
        context("get all user projects"){
            val userMemberships = buildList {
                repeat(Role.entries.size){ index->
                    add(ProjectMembershipDTO(projectDTO = createTestProject { name = "test project $index" }, role = Role.entries[index]))
                }
            }
            test("should get all user projects"){
                coEvery { projectRepository.findAllUserProjects(any()) } returns userMemberships
                projectService.getAllUserProjects(userDTO.phone) shouldBeRight userMemberships
                coVerify(exactly = 1) { projectRepository.findAllUserProjects(any()) }
            }
            test("should return Database error on ExposedSQLException"){
                val databaseException = ExposedSQLException(null,emptyList(),mockk())
                coEvery { projectRepository.findAllUserProjects(any()) } throws databaseException
                projectService.getAllUserProjects(userDTO.phone) shouldBeLeft FindAllUserProjectsException.Database(databaseException)
                coVerify(exactly = 1) { projectRepository.findAllUserProjects(any()) }
            }
            test("should return Unknown error on any throwable"){
                val exception = Throwable()
                coEvery { projectRepository.findAllUserProjects(any()) } throws exception
                projectService.getAllUserProjects(userDTO.phone) shouldBeLeft FindAllUserProjectsException.Unexpected(exception)
                coVerify(exactly = 1) { projectRepository.findAllUserProjects(any()) }
            }
        }
        context("join project"){
            val invitedUser = createTestUser { phone = "89038518685" }
            val project = createTestProject { name = "test project" }
            val invitation = createInvitation {
                fromInviter(userDTO.phone.value)
                toProject(project)
                withRole(Role.Admin)
            }
            test("should add user to project by invitation and delete invitation"){
                coEvery { invitationRepository.getInvitation(any()) } returns invitation
                coEvery { projectRepository.getProjectById(any()) } returns project
                coEvery { projectRepository.findUserMembershipInProject(any(),any()) } returns null
                coEvery { projectRepository.addMemberToProject(any(), any(),any()) } answers {
                    val projectDTO = secondArg<ProjectDTO>()
                    ProjectMembershipDTO(
                        projectDTO = projectDTO.copy(membersCount = projectDTO.membersCount+1),
                        role = thirdArg()
                    )
                }
                coEvery { invitationRepository.deleteInvitation(any()) } returns 1
                projectService.joinProject(invitedUser.phone,invitation.inviteCode.toStringUUID()) shouldBeRight project.copy(membersCount = project.membersCount + 1)
                coVerifyOrder {
                    invitationRepository.getInvitation(eq(invitation.inviteCode))
                    projectRepository.getProjectById(eq(project.projectID.toUUID()))
                    projectRepository.findUserMembershipInProject(eq(invitedUser.phone),project.projectID.toUUID())
                    projectRepository.addMemberToProject(eq(invitedUser.phone),eq(project), eq(invitation.role))
                    invitationRepository.deleteInvitation(invitation.inviteCode)
                }
            }
            test("should return InvitationNotFound if invitation not exists"){
                coEvery { invitationRepository.getInvitation(any()) } returns null
                projectService.joinProject(invitedUser.phone,invitation.inviteCode.toStringUUID()) shouldBeLeft JoinProjectException.InvitationNotFound(
                    invitation.inviteCode.toString()
                )
                coVerify(exactly = 1) { invitationRepository.getInvitation(any()) }
                coVerify(exactly = 0) { projectRepository.addMemberToProject(any(), any(),any()) }
            }
            test("should return InvitationExpired if invitation expire time < now time"){
                coEvery { invitationRepository.getInvitation(any()) } returns invitation.copy(expireAt = Clock.System.now().toEpochMilliseconds() - 1.days.inWholeMilliseconds)
                projectService.joinProject(invitedUser.phone,invitation.inviteCode.toStringUUID()) shouldBeLeft JoinProjectException.InvitationExpired(
                    invitation.inviteCode.toString()
                )
                coVerify(exactly = 1) { invitationRepository.getInvitation(any()) }
                coVerify(exactly = 0) { projectRepository.addMemberToProject(any(), any(),any()) }
            }
            test("should return ProjectNotFound if project not exists"){
                coEvery { invitationRepository.getInvitation(any()) } returns invitation
                coEvery { projectRepository.getProjectById(any()) } returns null
                projectService.joinProject(invitedUser.phone,invitation.inviteCode.toStringUUID()) shouldBeLeft JoinProjectException.ProjectNotFound(
                    invitation.projectID.toString()
                )
                coVerify(exactly = 1) { invitationRepository.getInvitation(any()) }
                coVerify(exactly = 1) { projectRepository.getProjectById(any()) }
                coVerify(exactly = 0) { projectRepository.addMemberToProject(any(), any(),any()) }
            }
            test("should return UserAlreadyProjectMember if user already stay in project"){
                coEvery { invitationRepository.getInvitation(any()) } returns invitation
                coEvery { projectRepository.getProjectById(any()) } returns project
                projectService.joinProject(userDTO.phone,invitation.inviteCode.toStringUUID()) shouldBeLeft JoinProjectException.UserAlreadyProjectMember(
                    invitation.projectID.toString()
                )
                coVerify(exactly = 1) { invitationRepository.getInvitation(any()) }
                coVerify(exactly = 0) { projectRepository.addMemberToProject(any(), any(),any()) }
            }
            test("should return Database error on ExposedSQLException"){
                val databaseException = ExposedSQLException(null,emptyList(),mockk())
                coEvery { invitationRepository.getInvitation(any()) } throws databaseException
                projectService.joinProject(invitedUser.phone,invitation.inviteCode.toStringUUID()) shouldBeLeft JoinProjectException.Database(
                    databaseException,
                )
                coVerify(exactly = 1) { invitationRepository.getInvitation(any()) }
                coVerify(exactly = 0) { projectRepository.addMemberToProject(any(), any(),any()) }
            }
            test("should return Unknown error on any throwable"){
               val throwable = Throwable()
                coEvery { invitationRepository.getInvitation(any()) } throws throwable
                projectService.joinProject(invitedUser.phone,invitation.inviteCode.toStringUUID()) shouldBeLeft JoinProjectException.Unexpected(
                    throwable,
                )
                coVerify(exactly = 1) { invitationRepository.getInvitation(any()) }
                coVerify(exactly = 0) { projectRepository.addMemberToProject(any(), any(),any()) }
            }
        }
    }
}