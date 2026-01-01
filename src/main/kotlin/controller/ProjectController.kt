package com.mapprjct.controller

import com.mapprjct.database.repositoryImpl.ProjectRepositoryImpl
import com.mapprjct.model.APISession
import com.mapprjct.service.ProjectService
import com.mapprjct.model.request.CreateProjectRequest
import com.mapprjct.model.request.InviteUserRequest
import com.mapprjct.model.request.JoinToProjectRequest
import com.mapprjct.model.response.CreateInvitationResponse
import com.mapprjct.model.response.GetAllUserProjectsResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.configureProjectsController() {
    val projectDAOImpl : ProjectRepositoryImpl by inject()
    val projectService: ProjectService by inject()
    routing() {
        authenticate("auth-session") {
            route("/projects") {
                post("/create") {
                    val session = call.principal<APISession>()!!
                    val userPhone = session.phone
                    val request = call.receive<CreateProjectRequest>()
                    val newProject = projectService.createProject(
                        creatorPhone = userPhone,
                        projectName = request.projectName
                    ).fold(
                        onSuccess = { result->
                            call.respond(status = HttpStatusCode.Created, message = result)
                                    },
                        onFailure = { error-> call.respond(HttpStatusCode.InternalServerError,error ) }
                    )
                    call.respond(status = HttpStatusCode.Created, message = newProject)

                }
                get("/") {
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
                    val invitation = projectService.createInvitation(
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
                    //todo incorrect phone saved : remove 8, check user already in project
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