package com.mapprjct.database.repository

import com.mapprjct.model.dto.ProjectDTO
import com.mapprjct.model.dto.ProjectMembershipDTO
import com.mapprjct.model.datatype.Role
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.StringUUID
import com.mapprjct.model.dto.ProjectRegistrationResultDTO
import com.mapprjct.model.dto.UnregisteredProjectDTO
import java.util.UUID

interface ProjectRepository {

    suspend fun findUserMembershipInProject(userPhone: RussiaPhoneNumber, projectID: UUID): ProjectMembershipDTO?
    /**
     * Select all uer projects and role in them
     * */
    suspend fun findAllUserProjects(userPhone: RussiaPhoneNumber) : List<ProjectMembershipDTO>
    /**
     * Create new project and insert record in ProjectUsersTable with Owner role
     * @throws java.sql.SQLIntegrityConstraintViolationException - if project name is empty string
     * */
    suspend fun insert(creatorPhone : RussiaPhoneNumber, project: UnregisteredProjectDTO) : ProjectRegistrationResultDTO

    suspend fun insertAll(userPhone : RussiaPhoneNumber,projects: List<UnregisteredProjectDTO>) : List<ProjectRegistrationResultDTO>

    /**
     * Try to find project with ID
     *
     * @return the project if it exists, null otherwise
     */
    suspend fun getProjectById(projectId : UUID) : ProjectDTO?
    /**
     * Insert record in ProjectUsersTable and increment members count in project
     */
    suspend fun addMemberToProject(userPhone : RussiaPhoneNumber, projectDTO: ProjectDTO, role : Role) : ProjectMembershipDTO
}