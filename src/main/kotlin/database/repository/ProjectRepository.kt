package com.mapprjct.database.repository

import com.mapprjct.model.Invitation
import com.mapprjct.model.dto.Project
import com.mapprjct.model.dto.ProjectMembership
import com.mapprjct.model.datatype.Role
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.StringUUID
import com.mapprjct.model.dto.UnregisteredProject
import java.util.UUID

interface ProjectRepository {

    suspend fun findUserMembershipInProject(userPhone: RussiaPhoneNumber, projectID: UUID): ProjectMembership?
    /**
     * Select all uer projects and role in them
     * */
    suspend fun findAllUserProjects(userPhone: RussiaPhoneNumber) : List<ProjectMembership>
    /**
     * Create new project and insert record in ProjectUsersTable with Owner role
     * @throws java.sql.SQLIntegrityConstraintViolationException - if project name is empty string
     * */
    suspend fun insert(creatorPhone : RussiaPhoneNumber, project: UnregisteredProject) : Pair<Project, StringUUID?>

    suspend fun insertAll(userPhone : RussiaPhoneNumber,projects: List<UnregisteredProject>) : List<Pair<Project, StringUUID?>>

    /**
     * Try to find project with ID
     *
     * @return the project if it exists, null otherwise
     */
    suspend fun getProjectById(projectId : UUID) : Project?
    /**
     * Insert record in ProjectUsersTable and increment members count in project
     */
    suspend fun addMemberToProject(userPhone : RussiaPhoneNumber, project: Project, role : Role)
}