package com.mapprjct.database.dao

import com.mapprjct.dto.Project
import com.mapprjct.dto.ProjectWithRole
import com.mapprjct.dto.Role
import java.util.UUID

interface ProjectRepository {
    suspend fun getAllUserProjects(userPhone: String) : List<ProjectWithRole>
    suspend fun insertProject(creatorPhone : String, projectName: String) : Project
    suspend fun getProjectById(projectId : UUID) : Project?
    suspend fun addMemberToProject(userPhone : String, project: Project, role : Role)
}