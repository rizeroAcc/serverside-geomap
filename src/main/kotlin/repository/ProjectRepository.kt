package com.mapprjct.repository

import com.mapprjct.database.dao.ProjectDAO
import com.mapprjct.dto.ProjectDTO

class ProjectRepository(val projectDAO: ProjectDAO) {
    suspend fun deleteProject(userPhone : String, projectDTO: ProjectDTO) : Result<Boolean> {
        TODO()
    }
    suspend fun createProject(creatorPhone : String, projectDTO : ProjectDTO) : Result<ProjectDTO> {
        TODO()
    }
    suspend fun updateProject(userPhone: String, projectDTO : ProjectDTO) : Result<ProjectDTO> {
        TODO()
    }
    suspend fun getProject(userPhone : String) : Result<ProjectDTO> {
        TODO()
    }
}