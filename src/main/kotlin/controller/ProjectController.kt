package com.mapprjct.controller

import com.mapprjct.controller.util.respondBadRequest
import com.mapprjct.controller.util.respondConflict
import com.mapprjct.controller.util.respondDatabaseError
import com.mapprjct.controller.util.respondElementNotFound
import com.mapprjct.controller.util.respondForbidden
import com.mapprjct.controller.util.respondUnexpected
import com.mapprjct.exceptions.domain.invitation.CreateInvitationException
import com.mapprjct.exceptions.domain.project.CreateProjectException
import com.mapprjct.exceptions.domain.project.FindAllUserProjectsException
import com.mapprjct.exceptions.domain.project.FindProjectException
import com.mapprjct.exceptions.domain.project.JoinProjectException
import com.mapprjct.model.APISession
import com.mapprjct.service.ProjectService
import com.mapprjct.model.request.project.CreateProjectRequest
import com.mapprjct.model.request.project.CreateInvitationRequest
import com.mapprjct.model.request.project.JoinProjectRequest
import com.mapprjct.model.ErrorResponse
import com.mapprjct.model.asRole
import com.mapprjct.model.createInvitationResponseFromInvitation
import com.mapprjct.model.response.project.CreateProjectResponse
import com.mapprjct.model.response.project.GetAllUserProjectsResponse
import com.mapprjct.model.response.project.GetProjectResponse
import com.mapprjct.model.value.RussiaPhoneNumber
import com.mapprjct.model.value.StringUUID
import com.mapprjct.service.InvitationService
import com.mapprjct.utils.fold
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.koin.ktor.ext.inject

fun Application.configureProjectsController() {
    val projectService: ProjectService by inject()
    val invitationService: InvitationService by inject()
    routing() {
        authenticate("auth-session") {
            route("/projects") {
                createProject(projectService)
                getProject(projectService)
                getAllProjects(projectService)
                inviteToProject(invitationService)
                joinToProject(projectService)
            }
        }
    }
}
fun Route.createProject(projectService: ProjectService) {
    post("") {
        val session = call.principal<APISession>()!!
        val userPhone = RussiaPhoneNumber(session.phone)
        val request = call.receive<CreateProjectRequest>()
        projectService.createProject(
            creatorPhone = userPhone,
            projectName = request.projectName
        ).fold(
            onSuccess = { result-> call.respond(status = HttpStatusCode.Created, message = CreateProjectResponse(result)) },
            onError = { error->
                when (error) {
                    is CreateProjectException.Database -> respondDatabaseError()
                    is CreateProjectException.InvalidProjectName -> respondBadRequest(message = error.message)
                    is CreateProjectException.Unexpected -> respondUnexpected()
                    is CreateProjectException.UserNotFound -> respondUnexpected()
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
            onSuccess = { result->
                call.respond(HttpStatusCode.OK, GetProjectResponse(result))
            },
            onError = { error ->
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
            onSuccess = { projectsAndRoles ->
                call.respond(HttpStatusCode.OK, message = GetAllUserProjectsResponse(projectsAndRoles))
            },
            onError = { error ->
                when (error) {
                    is FindAllUserProjectsException.Database -> respondDatabaseError()
                    is FindAllUserProjectsException.Unexpected -> respondUnexpected()
                    is FindAllUserProjectsException.UserNotFound -> respondUnexpected()
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
            onSuccess = { result->
                val response = createInvitationResponseFromInvitation(result)
                call.respond(status = HttpStatusCode.Created, message = response)
            },
            onError = { error->
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
fun Route.joinToProject(projectService: ProjectService) {
    post("/join") {
        val session = call.principal<APISession>()!!
        val request = call.receive<JoinProjectRequest>()
        val userPhone = RussiaPhoneNumber(session.phone)
        projectService.joinProject(userPhone, StringUUID(request.inviteCode)).fold(
            onSuccess = { result -> call.respond(HttpStatusCode.Accepted, message = result) },
            onError = { error ->
                when (error) {
                    is JoinProjectException.Database -> respondDatabaseError()
                    is JoinProjectException.InvitationNotFound -> respondElementNotFound("Invitation with code: ${error.invitationCode} not found")
                    is JoinProjectException.ProjectNotFound -> respondElementNotFound("Project with id: ${error.projectID} not found")
                    is JoinProjectException.Unexpected -> respondUnexpected()
                    is JoinProjectException.UserAlreadyProjectMember -> respondConflict("User already stay in project with id: ${error.projectID}")
                }
            },
        )
    }
}