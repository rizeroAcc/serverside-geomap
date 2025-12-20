package com.mapprjct.controller

import com.mapprjct.database.daoimpl.ProjectDAOImpl
import com.mapprjct.model.APISession
import com.mapprjct.dto.ProjectWithRole
import com.mapprjct.dto.User
import com.mapprjct.request.CreateProjectRequest
import com.mapprjct.response.GetAllUserProjectsResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.configureProjectsController() {
    val projectDAOImpl : ProjectDAOImpl by inject()
    routing() {
        authenticate("auth-session") {
            route("/projects") {
                post("/create") {
                    val session = call.principal<APISession>()!!
                    val userPhone = session.phone
                    val request = call.receive<CreateProjectRequest>()
                    val newProject = projectDAOImpl.createProject(
                        creatorPhone = userPhone,
                        projectName = request.projectName
                    )
                    if (newProject != null) {
                        call.respond(status = HttpStatusCode.Created, message = newProject)
                    }else{
                        call.respond(status = HttpStatusCode.Conflict, message = "Project already exists")
                    }

                }
                get("/") {
                    val session = call.principal<APISession>()!!
                    val userPhone = session.phone
                    val projectsAndRoles = projectDAOImpl.getAllUserProjects(userPhone = userPhone)
                    val response = GetAllUserProjectsResponse(
                        projectsAndRoles.map {
                            ProjectWithRole(
                                project = it.first,
                                role = it.second.toInt()
                            )
                        }
                    )
                    call.respond(HttpStatusCode.OK, message = response)
                }
            }
        }

    }
}