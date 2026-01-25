package com.mapprjct.kotest.controller

import com.mapprjct.getBean
import com.mapprjct.model.dto.Project
import com.mapprjct.model.dto.User
import com.mapprjct.model.request.auth.RegistrationRequest
import com.mapprjct.model.request.auth.SignInRequest
import com.mapprjct.model.request.project.CreateProjectRequest
import com.mapprjct.model.response.auth.RegistrationResponse
import com.mapprjct.model.response.project.GetProjectResponse
import com.mapprjct.service.ProjectService
import com.mapprjct.testKtorApp
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.http.parameters
import io.ktor.server.testing.ApplicationTestBuilder
import org.testcontainers.containers.PostgreSQLContainer

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
        ) : Pair<User,String>{
            val registrationRequest = RegistrationRequest(phone, username, password)
            val registrationResponse = client.post("/register") {
                setBody(registrationRequest)
            }.body<RegistrationResponse>()
            val signInResponse = client.post("/signin") {
                setBody(SignInRequest(phone, password))
            }
            return registrationResponse.user to signInResponse.headers["Authorization"]!!
        }
        context("create project"){
            testKtorApp(postgres){
                val projectService = getBean<ProjectService>()
                val (user,token) = createRegisterAndLoginUser()
                test("should create new project"){
                    val createProjectRequest = CreateProjectRequest(
                        projectName = "test"
                    )
                    val response = client.post("/projects") {
                        headers.append("Authorization", token)
                        setBody(createProjectRequest)
                    }
                    response shouldHaveStatus HttpStatusCode.Created
                    val project = response.body<Project>()
                    projectService.getProject(project.projectID).getOrThrow() shouldBe project
                }
                test("should respond BadRequest if project name empty"){
                    val createProjectRequest = CreateProjectRequest(
                        projectName = "   "
                    )
                    val response = client.post("/projects") {
                        headers.append("Authorization", token)
                        setBody(createProjectRequest)
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
                    setBody(CreateProjectRequest("testProject"))
                }.body<Project>()
                test("should get project"){
                    val response = client.get("/projects/${project.projectID}") {
                        headers.append("Authorization", token)
                    }
                    response shouldHaveStatus HttpStatusCode.OK
                    response.body<GetProjectResponse>().project shouldBe project
                }
            }
        }
    }
}