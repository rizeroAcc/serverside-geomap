package com.mapprjct.controller

import com.mapprjct.exceptions.invitation.InvitationDMLExceptions
import com.mapprjct.exceptions.invitation.InvitationValidationException
import com.mapprjct.exceptions.project.ProjectDMLException
import com.mapprjct.exceptions.project.ProjectValidationException
import com.mapprjct.model.APISession
import com.mapprjct.service.ProjectService
import com.mapprjct.model.request.project.CreateProjectRequest
import com.mapprjct.model.request.project.CreateInvitationRequest
import com.mapprjct.model.request.project.JoinProjectRequest
import com.mapprjct.model.ErrorResponse
import com.mapprjct.model.createInvitationResponseFromInvitation
import com.mapprjct.model.response.project.CreateProjectResponse
import com.mapprjct.model.response.project.GetAllUserProjectsResponse
import com.mapprjct.model.response.project.GetProjectResponse
import com.mapprjct.service.InvitationService
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
        val request = call.receive<CreateProjectRequest>()
        val newProject = projectService.createProject(
            creatorPhone = session.phone,
            projectName = request.projectName
        ).fold(
            onSuccess = { result-> call.respond(status = HttpStatusCode.Created, message = CreateProjectResponse(result)) },
            onFailure = { error->
                when (error) {
                    is ProjectValidationException.EmptyProjectName ->respondBadRequest("Project name is empty")
                    is ExposedSQLException -> logDatabaseErrorAndRespondISE(error)
                    else -> logErrorAndRespondISE(error, "Unknown error")
                }
            }
        )
    }
}
fun Route.getProject(projectService: ProjectService) {
    get("/{id}"){
        val projectId = call.pathParameters["id"]
        if(projectId == null){
            respondBadRequest("Missing project id")
            return@get
        }
        projectService.getProject(projectId).fold(
            onSuccess = { result->
                call.respond(HttpStatusCode.OK, GetProjectResponse(result))
            },
            onFailure = { exception ->
                when(exception) {
                    is IllegalArgumentException -> respondBadRequest("Invalid project id")
                    is ProjectDMLException.ProjectNotFoundException -> call.respond(HttpStatusCode.NotFound)
                    is ExposedSQLException -> logDatabaseErrorAndRespondISE(exception)
                    else -> logErrorAndRespondISE(exception, "Unknown error")
                }
            }
        )
    }
}
fun Route.getAllProjects(projectService: ProjectService) {
    get("/all") {
        val session = call.principal<APISession>()!!
        val userPhone = session.phone
        projectService.getAllUserProjects(userPhone = userPhone).fold(
            onSuccess = { projectsAndRoles->
                call.respond(HttpStatusCode.OK, message = GetAllUserProjectsResponse(projectsAndRoles))
            },
            onFailure = { error->
                call.respond(HttpStatusCode.InternalServerError, error.message.toString())
            }
        )
    }
}
fun Route.inviteToProject(invitationService: InvitationService) {
    post("/invite") {
        val session = call.principal<APISession>()!!
        val request = call.receive<CreateInvitationRequest>()
        invitationService.createInvitation(
            inviterPhone = session.phone,
            projectID = request.projectID,
            role = request.role
        ).fold(
            onSuccess = { result->
                val response = createInvitationResponseFromInvitation(result)
                call.respond(status = HttpStatusCode.Created, message = response)
            },
            onFailure = { error->
                when (error){
                    is ProjectDMLException.ProjectNotFoundException -> call.respond(
                        status = HttpStatusCode.NotFound,
                        message = ErrorResponse.fromText("Project with id ${request.projectID} not found")
                    )
                    is InvitationValidationException.InvalidUserRole -> respondBadRequest("Can't create invitation with role Owner")
                    is InvitationValidationException.UserNotStayInProject, is InvitationValidationException.NoPermissionToAddMembers -> call.respond(
                        status = HttpStatusCode.Forbidden,
                        message = ErrorResponse.fromAppException(error)
                    )
                    is IllegalArgumentException -> respondBadRequest(error.message.toString())
                    is InvitationValidationException.TooManyInvitationsPerUser -> call.respond(HttpStatusCode.Conflict, ErrorResponse.fromAppException(error))
                }
            }
        )
    }
}
fun Route.joinToProject(projectService: ProjectService) {
    post("/join") {

        val session = call.principal<APISession>()!!
        val request = call.receive<JoinProjectRequest>()
        val userPhone = session.phone
        projectService.joinProject(userPhone,request.inviteCode).fold(
            onSuccess = { result-> call.respond(HttpStatusCode.Accepted, message = result) },
            onFailure = { error->
                when(error){
                    is InvitationDMLExceptions.InvitationNotFoundException -> call.respond(HttpStatusCode.NotFound, ErrorResponse.fromAppException(error))
                    is ProjectValidationException.UserAlreadyProjectMember -> call.respond(HttpStatusCode.Conflict, ErrorResponse.fromAppException(error))
                    is IllegalArgumentException -> respondBadRequest(error.message.toString())
                    else -> logErrorAndRespondISE(error, "Unknown error")
                }
            }
        )
    }
}