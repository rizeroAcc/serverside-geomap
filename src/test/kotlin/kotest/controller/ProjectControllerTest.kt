package com.mapprjct.kotest.controller

import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.getBean
import com.mapprjct.model.dto.ProjectDTO
import com.mapprjct.model.dto.UserDTO
import com.mapprjct.model.request.auth.RegistrationRequest
import com.mapprjct.model.request.auth.SignInRequest
import com.mapprjct.model.request.project.CreateInvitationRequest
import com.mapprjct.model.request.project.RegisterProjectRequest
import com.mapprjct.model.request.project.JoinProjectRequest
import com.mapprjct.model.response.auth.RegistrationResponse
import com.mapprjct.model.ErrorResponse
import com.mapprjct.model.datatype.Password
import com.mapprjct.model.datatype.Role
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.Username
import com.mapprjct.model.dto.UnregisteredProjectDTO
import com.mapprjct.model.response.project.CreateInvitationResponse
import com.mapprjct.model.response.project.RegisterProjectResponse
import com.mapprjct.model.response.project.GetAllUserProjectsResponse
import com.mapprjct.model.response.project.GetProjectResponse
import com.mapprjct.model.toInvitation
import com.mapprjct.service.InvitationService
import com.mapprjct.service.ProjectService
import com.mapprjct.testKtorApp
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.testcontainers.containers.PostgreSQLContainer
import java.util.UUID

class ProjectControllerTest : FunSpec() {
    init {
        val postgres = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("testdb")
            .withUsername("postgres")
            .withPassword("postgres")
            .withReuse(true)

        install(TestContainerSpecExtension(postgres))

        suspend fun ApplicationTestBuilder.createRegisterAndLoginUser(
            phone : String = "89036559989",
            username : String = "admin",
            password : String = "testPassword"
        ) : Pair<UserDTO,String>{
            val registrationRequest = RegistrationRequest(
                RussiaPhoneNumber(phone),
                Username(username),
                Password(password)
            )
            val registrationResponse = client.post("/register") {
                setBody(registrationRequest)
            }.body<RegistrationResponse>()
            val signInResponse = client.post("/signin") {
                setBody(SignInRequest(
                    RussiaPhoneNumber(phone),
                    Password(password)
                ))
            }
            return registrationResponse.userDTO to signInResponse.headers["Authorization"]!!
        }

        context("create project"){
            testKtorApp(postgres){
                val projectService = getBean<ProjectService>()
                val (user,token) = createRegisterAndLoginUser()
                test("should create new project"){
                    val registerProjectRequest = RegisterProjectRequest(
                        UnregisteredProjectDTO(name = "projectName")
                    )
                    val response = client.post("/projects") {
                        headers.append("Authorization", token)
                        setBody(registerProjectRequest)
                    }
                    response shouldHaveStatus HttpStatusCode.Created
                    val project = response.body<RegisterProjectResponse>().registrationResult.projectDTO
                    projectService.getProject(project.projectID) shouldBeRight project
                }
                test("should respond BadRequest if project name empty"){
                    val registerProjectRequest = RegisterProjectRequest(
                        UnregisteredProjectDTO(name = "    ")
                    )
                    val response = client.post("/projects") {
                        headers.append("Authorization", token)
                        setBody(registerProjectRequest)
                    }
                    response shouldHaveStatus HttpStatusCode.BadRequest
                }
            }
        }
        context("get project"){
            testKtorApp(postgres){
                val (user,token) = createRegisterAndLoginUser()
                val project = client.post("/projects") {
                    headers.append("Authorization", token)
                    setBody(RegisterProjectRequest(UnregisteredProjectDTO(name = "testProject")))
                }.body<RegisterProjectResponse>().registrationResult.projectDTO
                test("should get project"){
                    val response = client.get("/projects/${project.projectID.value}") {
                        headers.append("Authorization", token)
                    }
                    response shouldHaveStatus HttpStatusCode.OK
                    response.body<GetProjectResponse>().projectDTO shouldBe project
                }
                test("should respond BadRequest if project id invalid"){
                    val invalidUUID = UUID.randomUUID().toString().replace("-","")
                    client.get("/projects/${invalidUUID}") {
                        headers.append("Authorization", token)
                    } shouldHaveStatus HttpStatusCode.BadRequest
                }
                test("should respond Not Found if project with ID doesn't exist"){
                    val unexistingProjectID = UUID.randomUUID().toString()
                    client.get("/projects/${unexistingProjectID}") {
                        headers.append("Authorization", token)
                    } shouldHaveStatus HttpStatusCode.NotFound
                }
            }
        }
        context("get all user projects"){
            testKtorApp(postgres){
                val (user,token) = createRegisterAndLoginUser()
                val projectDTOS = mutableListOf<ProjectDTO>()
                for(i in 1..5) {
                    val project = client.post("/projects") {
                        headers.append("Authorization", token)
                        setBody(RegisterProjectRequest(UnregisteredProjectDTO(name = "testProject$i")))
                    }.body<RegisterProjectResponse>().registrationResult.projectDTO
                    projectDTOS.add(project)
                }
                test("should get all user projects"){
                    val response = client.get("/projects/all") {
                        headers.append("Authorization", token)
                    }
                    response shouldHaveStatus HttpStatusCode.OK
                    response.body<GetAllUserProjectsResponse>()
                        .result.map {
                            it.projectDTO
                        } shouldContainAll projectDTOS
                }
            }
        }
        context("create invitation"){
            testKtorApp(postgres){
                val invitationService = getBean<InvitationService>()
                val (user,token) = createRegisterAndLoginUser(phone = "89036559989")
                val registerProjectRequest = RegisterProjectRequest(UnregisteredProjectDTO(name = "test"))
                val project = client.post("/projects") {
                    headers.append("Authorization", token)
                    setBody(registerProjectRequest)
                }.body<RegisterProjectResponse>().registrationResult.projectDTO

                test("should create new invitation"){
                    val createInvitationRequest = CreateInvitationRequest(
                        projectID = project.projectID.value,
                        role = Role.Worker.toShort()
                    )
                    val response = client.post("/projects/invite") {
                        headers.append("Authorization", token)
                        setBody(createInvitationRequest)
                    }
                    response shouldHaveStatus HttpStatusCode.Created
                    val invitationDTO = response.body<CreateInvitationResponse>().invitationDTO
                    invitationService.getInvitation(invitationDTO.inviteCode) shouldBeRight invitationDTO.toInvitation()
                }
                test("should respond BadRequest if project id isn't UUID"){
                    val createInvitationRequest = CreateInvitationRequest(
                        projectID = UUID.randomUUID().toString().replace("-",""),
                        role = Role.Worker.toShort()
                    )
                    val response = client.post("/projects/invite") {
                        headers.append("Authorization", token)
                        setBody(createInvitationRequest)
                    }
                    response shouldHaveStatus HttpStatusCode.BadRequest
                    response.body<ErrorResponse>().message shouldContain "Invalid UUID format"
                }
                test("should respond BadRequest if user role code invalid"){
                    val maxRoleOrdinal = Role.entries.maxBy { it.ordinal }.ordinal + 1
                    val createInvitationRequest = CreateInvitationRequest(
                        projectID = project.projectID.value,
                        role = (maxRoleOrdinal + 1).toShort()
                    )
                    val response = client.post("/projects/invite") {
                        headers.append("Authorization", token)
                        setBody(createInvitationRequest)
                    }
                    response shouldHaveStatus HttpStatusCode.BadRequest
                    response.body<ErrorResponse>().message shouldContain "Unknown role"
                }
                test("should respond BadRequest if try to create invitation with role Owner"){
                    val createInvitationRequest = CreateInvitationRequest(
                        projectID = project.projectID.value,
                        role = Role.Owner.toShort()
                    )
                    val response = client.post("/projects/invite") {
                        headers.append("Authorization", token)
                        setBody(createInvitationRequest)
                    }
                    response shouldHaveStatus HttpStatusCode.BadRequest
                    response.body<ErrorResponse>().message shouldContain "Cannot invite with role: Owner"
                }
                test("should respond Not Found if project with ID doesn't exist"){
                    val createInvitationRequest = CreateInvitationRequest(
                        projectID = UUID.randomUUID().toString(),
                        role = Role.Worker.toShort()
                    )
                    val response = client.post("/projects/invite") {
                        headers.append("Authorization", token)
                        setBody(createInvitationRequest)
                    }
                    response shouldHaveStatus HttpStatusCode.NotFound
                }
                test("should respond Forbidden if user doesn't stay in project"){
                    val (secondUser, secondToken) = createRegisterAndLoginUser(phone = "89038518685")
                    val createInvitationRequest = CreateInvitationRequest(
                        projectID = project.projectID.value,
                        role = Role.Worker.toShort()
                    )
                    val response = client.post("/projects/invite") {
                        headers.append("Authorization", secondToken)
                        setBody(createInvitationRequest)
                    }
                    response shouldHaveStatus HttpStatusCode.Forbidden
                    response.body<ErrorResponse>().message shouldBe "User not stay in project ${project.projectID.value}"
                }
            }
            testKtorApp(postgres){

                val projectRepository = getBean<ProjectRepository>()
                val (user,token) = createRegisterAndLoginUser(phone = "89036559989")
                val (secondUser, secondToken) = createRegisterAndLoginUser(phone = "89038518685")
                val registerProjectRequest = RegisterProjectRequest(UnregisteredProjectDTO(name = "test"))
                val project = client.post("/projects") {
                    headers.append("Authorization", token)
                    setBody(registerProjectRequest)
                }.body<RegisterProjectResponse>().registrationResult.projectDTO

                test("should respond Forbidden if user haven't permission to add members"){
                    suspendTransaction {
                        projectRepository.addMemberToProject(secondUser.phone,project,role = Role.Worker)
                    }
                    val createInvitationRequest = CreateInvitationRequest(
                        projectID = project.projectID.value,
                        role = Role.Worker.toShort()
                    )
                    val response = client.post("/projects/invite") {
                        headers.append("Authorization", secondToken)
                        setBody(createInvitationRequest)
                    }
                    response shouldHaveStatus HttpStatusCode.Forbidden
                    response.body<ErrorResponse>().message shouldBe "User hasn't permission add members to project ${project.projectID.value}"
                }
            }
        }
        context("join project"){
            testKtorApp(postgres){
                val invitationService = getBean<InvitationService>()
                val projectService = getBean<ProjectService>()

                val (user,token) = createRegisterAndLoginUser(phone = "89036559989")
                val (invitedUser,invitedUserToken) = createRegisterAndLoginUser(phone = "89038518685")
                val project = client.post("/projects"){
                    headers.append("Authorization", token)
                    setBody(RegisterProjectRequest(UnregisteredProjectDTO(name = "test")))
                }.body<RegisterProjectResponse>().registrationResult.projectDTO
                test("should join project"){
                    val invitationCode = invitationService.createInvitation(
                        user.phone,project.projectID, Role.Worker
                    ).getOrNull()!!.inviteCode
                    val response = client.post("/projects/join"){
                        headers.append("Authorization", invitedUserToken)
                        setBody(JoinProjectRequest(invitationCode.toString()))
                    }
                    response shouldHaveStatus HttpStatusCode.Accepted
                    projectService.getAllUserProjects(invitedUser.phone)
                        .getOrNull()!!
                        .map {
                            it.projectDTO.projectID
                        } shouldContain project.projectID
                }
                test("should respond Not Found if invitation not found"){
                    val wrongCode = UUID.randomUUID().toString()
                    client.post("/projects/join"){
                        headers.append("Authorization", invitedUserToken)
                        setBody(JoinProjectRequest(wrongCode))
                    } shouldHaveStatus HttpStatusCode.NotFound
                }
                test("should respond BadRequest if invitation code invalid"){
                    val invalidCode = UUID.randomUUID().toString().replace("-","")
                    client.post("/projects/join"){
                        headers.append("Authorization", invitedUserToken)
                        setBody(JoinProjectRequest(invalidCode))
                    } shouldHaveStatus HttpStatusCode.BadRequest
                }
                test("should respond Conflict if user already project member"){
                    val invitation = invitationService.createInvitation(
                        user.phone,project.projectID, Role.Worker
                    ).getOrNull()!!
                    client.post("/projects/join"){
                        headers.append("Authorization", token)
                        setBody(JoinProjectRequest(invitation.inviteCode.toString()))
                    } shouldHaveStatus HttpStatusCode.Conflict
                }
            }
        }
    }
}