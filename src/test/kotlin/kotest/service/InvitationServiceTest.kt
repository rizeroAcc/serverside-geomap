package com.mapprjct.kotest.service

import com.mapprjct.builders.createInvitation
import com.mapprjct.builders.createTestProject
import com.mapprjct.builders.createTestUser
import com.mapprjct.com.mapprjct.utils.BypassTransactionProvider
import com.mapprjct.database.repository.InvitationRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.tables.InviteCodeTable.projectID
import com.mapprjct.exceptions.domain.invitation.CreateInvitationException
import com.mapprjct.exceptions.domain.invitation.FindInvitationException
import com.mapprjct.model.datatype.Role
import com.mapprjct.model.dto.ProjectMembershipDTO
import com.mapprjct.service.InvitationService
import com.mapprjct.utils.toStringUUID
import com.mapprjct.utils.toUUID
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

class InvitationServiceTest : FunSpec() {

    init {
        val projectRepository : ProjectRepository = mockk()
        val invitationRepository : InvitationRepository = mockk()
        val invitationService = InvitationService(
            transactionProvider = BypassTransactionProvider(),
            projectRepository = projectRepository,
            invitationRepository = invitationRepository
        )
        beforeTest {
            clearAllMocks(answers = true, recordedCalls = true)
        }

        val userDTO = createTestUser {  }
        val project = createTestProject { name = "testProject" }

        context("create invitation") {
            val expectedInvitation = createInvitation {
                fromInviter(userDTO.phone.value)
                toProject(project)
                withRole(Role.Admin)
            }
            val inviterOwnerMembership = ProjectMembershipDTO(
                projectDTO = project,
                role = Role.Owner
            )
            test("should create invitation"){
                coEvery { projectRepository.getProjectById(any()) } returns project
                coEvery { projectRepository.findUserMembershipInProject(any(),any()) } returns inviterOwnerMembership

                coEvery {
                    invitationRepository.insertInvitation(any())
                } returns expectedInvitation
                invitationService.createInvitation(userDTO.phone,project.projectID,Role.Admin) shouldBeRight expectedInvitation
                coVerifyOrder{
                    projectRepository.getProjectById(any())
                    projectRepository.findUserMembershipInProject(any(),any())
                    invitationRepository.insertInvitation(
                        invitation = withArg { invitation->
                            invitation.role shouldBe Role.Admin
                            invitation.inviterPhone shouldBe userDTO.phone
                            invitation.projectID shouldBe project.projectID.toUUID()
                        }
                    )
                }
            }
            test("should return InvalidInvitationRole if try create invitation with role Owner"){
                invitationService.createInvitation(userDTO.phone,project.projectID,Role.Owner) shouldBeLeft CreateInvitationException.InvalidInvitationRole(Role.Owner)
                coVerify(exactly = 0) { invitationRepository.insertInvitation(any()) }
            }
            test("should return ProjectNotFound if project doesn't exists"){
                coEvery { projectRepository.getProjectById(any()) } returns null
                invitationService.createInvitation(userDTO.phone,project.projectID,Role.Admin) shouldBeLeft CreateInvitationException.ProjectNotFound(project.projectID.value)
                coVerify(exactly = 1) { projectRepository.getProjectById(project.projectID.toUUID()) }
                coVerify(exactly = 0) { invitationRepository.insertInvitation(any()) }
            }
            test("should return InviterNotStayInProject if user not stay in project"){
                coEvery { projectRepository.getProjectById(any()) } returns project
                coEvery { projectRepository.findUserMembershipInProject(any(),any()) } returns null
                invitationService.createInvitation(userDTO.phone,project.projectID,Role.Admin) shouldBeLeft CreateInvitationException.InviterNotStayInProject(project.projectID.value)
                coVerify(exactly = 1) {
                    projectRepository.getProjectById(any())
                    projectRepository.findUserMembershipInProject(any(),any())
                }
                coVerify(exactly = 0) { invitationRepository.insertInvitation(any()) }
            }
            test("should return NoPermissionToAddMembers if user hasn't permission to add members"){
                val inviterWorkerMembership = inviterOwnerMembership.copy(role = Role.Worker)
                coEvery { projectRepository.getProjectById(any()) } returns project
                coEvery { projectRepository.findUserMembershipInProject(any(),any()) } returns inviterWorkerMembership
                invitationService.createInvitation(userDTO.phone,project.projectID,Role.Admin) shouldBeLeft CreateInvitationException.NoPermissionToAddMembers(project.projectID.value)
                coVerify(exactly = 1) {
                    projectRepository.getProjectById(any())
                    projectRepository.findUserMembershipInProject(any(),any())
                }
                coVerify(exactly = 0) { invitationRepository.insertInvitation(any()) }
            }
            test("should return Database error on ExposedSQLException"){
                val databaseException = ExposedSQLException(null,emptyList(),mockk())
                coEvery { projectRepository.getProjectById(any()) } throws databaseException
                invitationService.createInvitation(userDTO.phone,project.projectID,Role.Admin) shouldBeLeft CreateInvitationException.Database(databaseException)
                coVerify(exactly = 0) { invitationRepository.insertInvitation(any()) }
            }
            test("should return Unknown error on unexpected throwable"){
                val exception = Throwable()
                coEvery { projectRepository.getProjectById(any()) } returns project
                coEvery { projectRepository.findUserMembershipInProject(any(),any()) } throws exception
                invitationService.createInvitation(userDTO.phone,project.projectID,Role.Admin) shouldBeLeft CreateInvitationException.Unexpected(exception)
                coVerify(exactly = 1) {
                    projectRepository.getProjectById(any())
                    projectRepository.findUserMembershipInProject(any(),any())
                }
                coVerify(exactly = 0) { invitationRepository.insertInvitation(any()) }
            }
        }
        context("get invitation"){
            val expectedInvitation = createInvitation {
                fromInviter(userDTO.phone.value)
                toProject(project)
                withRole(Role.Admin)
            }
            test("should get existing invitation"){
                coEvery { invitationRepository.getInvitation(any()) } returns expectedInvitation
                invitationService.getInvitation(expectedInvitation.inviteCode.toStringUUID()) shouldBeRight expectedInvitation
                coVerify(exactly = 1) {
                    invitationRepository.getInvitation(eq(expectedInvitation.inviteCode))
                }
            }
            test("should return NotFound if invitation not found"){
                coEvery { invitationRepository.getInvitation(any()) } returns null
                invitationService.getInvitation(expectedInvitation.inviteCode.toStringUUID()) shouldBeLeft FindInvitationException.NotFound(expectedInvitation.inviteCode.toString())
                coVerify(exactly = 1) {
                    invitationRepository.getInvitation(eq(expectedInvitation.inviteCode))
                }
            }
            test("should Database error on ExposedSQLException"){
                val databaseException = ExposedSQLException(null,emptyList(),mockk())
                coEvery { invitationRepository.getInvitation(any()) } throws databaseException
                invitationService.getInvitation(expectedInvitation.inviteCode.toStringUUID()) shouldBeLeft FindInvitationException.Database(databaseException)
                coVerify(exactly = 1) {
                    invitationRepository.getInvitation(eq(expectedInvitation.inviteCode))
                }
            }
            test("should return Unknown error on unexpected throwable"){
                val exception = Throwable()
                coEvery { invitationRepository.getInvitation(any()) } throws exception
                invitationService.getInvitation(expectedInvitation.inviteCode.toStringUUID()) shouldBeLeft FindInvitationException.Unexpected(exception)
                coVerify(exactly = 1) {
                    invitationRepository.getInvitation(eq(expectedInvitation.inviteCode))
                }
            }
        }
    }
}