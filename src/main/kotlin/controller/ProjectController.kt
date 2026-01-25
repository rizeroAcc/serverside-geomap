package com.mapprjct.controller

import com.mapprjct.database.repositoryImpl.ProjectRepositoryImpl
import com.mapprjct.exceptions.project.ProjectDMLException
import com.mapprjct.exceptions.project.ProjectValidationException
import com.mapprjct.exceptions.user.UserDMLExceptions
import com.mapprjct.model.APISession
import com.mapprjct.service.ProjectService
import com.mapprjct.model.request.project.CreateProjectRequest
import com.mapprjct.model.request.project.InviteUserRequest
import com.mapprjct.model.request.project.JoinToProjectRequest
import com.mapprjct.model.response.project.CreateInvitationResponse
import com.mapprjct.model.response.project.GetAllUserProjectsResponse
import com.mapprjct.model.response.project.GetProjectResponse
import com.mapprjct.service.InvitationService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.koin.ktor.ext.inject

fun Application.configureProjectsController() {
    val projectDAOImpl : ProjectRepositoryImpl by inject()
    val projectService: ProjectService by inject()
    val invitationService: InvitationService by inject()
    routing() {
        authenticate("auth-session") {
            route("/projects") {
                createProject(projectService)
                getProject(projectService)
                get("/all") {
                    val session = call.principal<APISession>()!!
                    val userPhone = session.phone
                    projectService.getAllUserProjects(userPhone = userPhone).fold(
                        onSuccess = { projectsAndRolse->
                            val response = GetAllUserProjectsResponse(
                                projectsAndRolse
                            )
                            call.respond(HttpStatusCode.OK, message = response)
                        },
                        onFailure = { error->
                            call.respond(HttpStatusCode.InternalServerError, error.message.toString())
                        }
                    )

                }
                post("/invite") {
                    val session = call.principal<APISession>()!!
                    val userPhone = session.phone
                    val request = call.receive<InviteUserRequest>()
                    val invitation = invitationService.createInvitation(
                        inviterPhone = userPhone,
                        projectID = request.projectID,
                        role = request.role
                    ).fold(
                        onSuccess = { result->
                            val response = CreateInvitationResponse.fromInvitation(result)
                            call.respond(status = HttpStatusCode.Created, message = response)
                        },
                        onFailure = { error->
                            when (error){
                                is NotFoundException ->
                                    call.respond(HttpStatusCode.NotFound, error.message.toString())
                                is IllegalArgumentException ->
                                    call.respond(HttpStatusCode.BadRequest, error.message.toString())
                                is IllegalStateException -> call.respond(HttpStatusCode.NotAcceptable,error.message.toString() )
                            }
                        }
                    )
                }
                post("/join") {
                    //todo check user already in project
                    val session = call.principal<APISession>()!!
                    val request = call.receive<JoinToProjectRequest>()
                    val userPhone = session.phone

                    val project = projectService.joinProject(userPhone,request.inviteCode).fold(
                        onSuccess = { result-> call.respond(HttpStatusCode.Accepted, message = result) },
                        onFailure = { error->
                            when(error){
                                is NotFoundException -> call.respond(HttpStatusCode.NotFound, error.message.toString())
                                is java.lang.IllegalStateException -> call.respond(HttpStatusCode.NotAcceptable, error.message.toString())
                                else -> call.respond(HttpStatusCode.InternalServerError,error.message.toString())
                            }
                        }
                    )

                }
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
            onSuccess = { result-> call.respond(status = HttpStatusCode.Created, message = result) },
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
                    is ProjectDMLException.ProjectNotFoundException -> call.respond(HttpStatusCode.NotFound)
                    is ExposedSQLException -> logDatabaseErrorAndRespondISE(exception)
                    else -> logErrorAndRespondISE(exception, "Unknown error")
                }
            }
        )
    }
}