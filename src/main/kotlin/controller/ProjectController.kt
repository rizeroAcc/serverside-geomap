package com.mapprjct.controller

import com.mapprjct.database.daoimpl.ProjectDAOImpl
import com.mapprjct.dto.Project
import com.mapprjct.dto.APISession
import com.mapprjct.dto.User
import com.mapprjct.request.CreateProjectRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.koin.ktor.ext.inject

fun Application.configureProjectsController() {
    val projectDAOImpl : ProjectDAOImpl by inject()
    routing() {
        route("/projects") {
            post("/create") {
                val session = call.sessions.get<APISession>()
                if (session == null) {
                    call.respond(status = HttpStatusCode.Unauthorized, message = "Unauthorized")
                }
                val userPhone = session!!.phone
                val request = call.receive<CreateProjectRequest>()
                val newProject = projectDAOImpl.createProject(creatorPhone = userPhone, project = Project(
                        name = request.projectName,
                        projectID = ""
                    )
                )
                if (newProject != null) {
                    call.respond(status = HttpStatusCode.Created, message = newProject)
                }else{
                    call.respond(status = HttpStatusCode.Conflict, message = "Project already exists")
                }

            }
            get("/") {
                val session = call.sessions.get<APISession>()
                if (session == null) {
                    call.respond(status = HttpStatusCode.Unauthorized, message = "Unauthorized")
                }
                val userPhone = session!!.phone
                val projects = projectDAOImpl.getAllUserProjects(User(
                    phone = userPhone,
                    username = ""
                ))
                call.respond(HttpStatusCode.OK, message = projects)
            }
        }
    }
}