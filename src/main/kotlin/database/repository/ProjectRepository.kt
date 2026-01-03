package com.mapprjct.database.repository

import com.mapprjct.model.dto.Project
import com.mapprjct.model.dto.ProjectWithRole
import com.mapprjct.model.dto.Role
import java.util.UUID

interface ProjectRepository {
    /**
     * Select all uer projects and role in them
     * */
    suspend fun getAllUserProjects(userPhone: String) : List<ProjectWithRole>
    /**
     * Create new project and insert record in ProjectUsersTable with Owner role
     * */
    suspend fun insertProject(creatorPhone : String, projectName: String) : Project
    /**
     * Try to find project with ID
     *
     * @return the project if it exists, null otherwise
     */
    suspend fun getProjectById(projectId : UUID) : Project?
    /**
     * Insert record in ProjectUsersTable and increment members count in project
     */
    suspend fun addMemberToProject(userPhone : String, project: Project, role : Role)
}