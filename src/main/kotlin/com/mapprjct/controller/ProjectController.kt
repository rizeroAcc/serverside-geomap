package com.mapprjct.controller

import com.mapprjct.controller.util.*
import com.mapprjct.exceptions.domain.invitation.CreateInvitationException
import com.mapprjct.exceptions.domain.project.FindAllUserProjectsException
import com.mapprjct.exceptions.domain.project.FindProjectException
import com.mapprjct.exceptions.domain.project.JoinProjectException
import com.mapprjct.exceptions.domain.project.ProjectRegistrationError
import com.mapprjct.model.APISession
import com.mapprjct.model.createInvitationResponseFromInvitation
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.StringUUID
import com.mapprjct.model.request.project.CreateInvitationRequest
import com.mapprjct.model.request.project.JoinProjectRequest
import com.mapprjct.model.request.project.RegisterProjectListRequest
import com.mapprjct.model.request.project.RegisterProjectRequest
import com.mapprjct.model.response.project.GetAllUserProjectsResponse
import com.mapprjct.model.response.project.GetProjectResponse
import com.mapprjct.model.response.project.RegisterProjectListResponse
import com.mapprjct.model.response.project.RegisterProjectResponse
import com.mapprjct.service.InvitationService
import com.mapprjct.service.ProjectService
import com.mapprjct.utils.asRole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.qualifier.named
import org.koin.ktor.ext.inject

fun Application.configureProjectsController() {
    val projectService: ProjectService by inject()
    val invitationService: InvitationService by inject()
    val scope : CoroutineScope by inject(named("Main background scope"))
    routing() {
        authenticate("auth-session") {
            route("/projects") {
                registerProject(projectService)
                registerProjectList(projectService)
                getProject(projectService)
                getAllProjects(projectService)
                inviteToProject(invitationService)
                joinToProject(projectService,invitationService,scope)
            }
        }
    }
}
fun Route.registerProject(projectService: ProjectService) {
    post("") {
        val session = call.principal<APISession>()!!
        val userPhone = RussiaPhoneNumber(session.phone)
        val request = call.receive<RegisterProjectRequest>()
        projectService.registerProject(
            creatorPhone = userPhone,
            unregisteredProjectDTO = request.project
        ).fold(
            ifRight = { result->
                val responseBody = RegisterProjectResponse(registrationResult = result)
                call.respond(status = HttpStatusCode.Created, message = responseBody)
            },
            ifLeft = { error->
                when (error) {
                    is ProjectRegistrationError.Database -> respondDatabaseError()
                    is ProjectRegistrationError.BlankProjectName -> respondBadRequest("Project name can't be blank")
                    is ProjectRegistrationError.Unexpected -> respondUnexpected()
                    is ProjectRegistrationError.UserNotFound -> respondUnexpected()
                }
            }
        )
    }
}

fun Route.registerProjectList(projectService: ProjectService) {
    post("/registerAll") {
        val session = call.principal<APISession>()!!
        val userPhone = RussiaPhoneNumber(session.phone)
        val request = call.receive<RegisterProjectListRequest>()
        projectService.registerProjectList(
            creatorPhone = userPhone,
            unregisteredProjectDTOS = request.projects
        ).fold(
            ifRight = { result->
                call.respond(
                    status = HttpStatusCode.Created,
                    message = RegisterProjectListResponse(result)
                )
            },
            ifLeft = { error->
                when (error) {
                    is ProjectRegistrationError.Database -> respondDatabaseError()
                    is ProjectRegistrationError.BlankProjectName -> respondBadRequest("Project name can't be blank")
                    is ProjectRegistrationError.Unexpected -> respondUnexpected()
                    is ProjectRegistrationError.UserNotFound -> respondUnexpected()
                }
            }
        )
    }
}
fun Route.getProject(projectService: ProjectService) {
    get("/{id}"){
        val projectId = call.pathParameters["id"]?.let { StringUUID(it) }
        if(projectId == null){
            respondBadRequest("Missing project id")
            return@get
        }
        projectService.getProject(projectId).fold(
            ifRight = { result->
                call.respond(HttpStatusCode.OK, GetProjectResponse(result))
            },
            ifLeft = { error ->
                when(error) {
                    is FindProjectException.Database -> respondDatabaseError()
                    is FindProjectException.NotFound -> respondElementNotFound("Project with id: ${error.projectID} not found")
                    is FindProjectException.Unexpected -> respondUnexpected()
                }
            }
        )
    }
}
fun Route.getAllProjects(projectService: ProjectService) {
    get("/all") {
        val session = call.principal<APISession>()!!
        val userPhone = RussiaPhoneNumber(session.phone)
        projectService.getAllUserProjects(userPhone).fold(
            ifRight = { projectsAndRoles ->
                call.respond(HttpStatusCode.OK, message = GetAllUserProjectsResponse(projectsAndRoles))
            },
            ifLeft = { error ->
                when (error) {
                    is FindAllUserProjectsException.Database -> respondDatabaseError()
                    is FindAllUserProjectsException.Unexpected -> respondUnexpected()
                }
            },
        )
    }
}
fun Route.inviteToProject(invitationService: InvitationService) {
    post("/invite") {
        val session = call.principal<APISession>()!!
        val userPhone = RussiaPhoneNumber(session.phone)
        val request = call.receive<CreateInvitationRequest>()
        invitationService.createInvitation(
            inviterPhone = userPhone,
            projectID = StringUUID(request.projectID),
            role = request.role.asRole()
        ).fold(
            ifRight = { result->
                val response = createInvitationResponseFromInvitation(result)
                call.respond(status = HttpStatusCode.Created, message = response)
            },
            ifLeft = { error->
                when (error){
                    is CreateInvitationException.Database -> respondDatabaseError()
                    is CreateInvitationException.InvalidInvitationRole -> respondBadRequest("Cannot invite with role: ${error.role.name}")
                    is CreateInvitationException.InviterNotStayInProject -> respondForbidden("User not stay in project ${request.projectID}")
                    is CreateInvitationException.NoPermissionToAddMembers -> respondForbidden("User hasn't permission add members to project ${request.projectID}")
                    is CreateInvitationException.ProjectNotFound -> respondElementNotFound("Project with id: ${error.projectID} not found")
                    is CreateInvitationException.Unexpected -> respondUnexpected()
                }
            }
        )
    }
}
fun Route.joinToProject(projectService: ProjectService, invitationService : InvitationService, scope: CoroutineScope) {
    post("/join") {
        val session = call.principal<APISession>()!!
        val request = call.receive<JoinProjectRequest>()
        val userPhone = RussiaPhoneNumber(session.phone)
        projectService.joinProject(userPhone, StringUUID(request.inviteCode)).fold(
            ifRight = { result -> call.respond(HttpStatusCode.Accepted, message = result) },
            ifLeft = { error ->
                when (error) {
                    is JoinProjectException.Database -> respondDatabaseError()
                    is JoinProjectException.InvitationNotFound -> respondElementNotFound("Invitation with code: ${error.invitationCode} not found")
                    is JoinProjectException.ProjectNotFound -> respondElementNotFound("Project with id: ${error.projectID} not found")
                    is JoinProjectException.Unexpected -> respondUnexpected()
                    is JoinProjectException.UserAlreadyProjectMember -> respondConflict("User already stay in project with id: ${error.projectID}")
                    is JoinProjectException.InvitationExpired -> {
                        scope.launch {
                            invitationService.deleteInvitation(error.invitationCode)
                        }
                        respondForbidden("Invitation expired")
                    }
                }
            },
        )
    }
}