package com.mapprjct.repository

import com.mapprjct.database.dao.ProjectDAO
import com.mapprjct.dto.Project

class ProjectRepository(val projectDAO: ProjectDAO) {
    suspend fun deleteProject(userPhone : String, project: Project) : Result<Boolean> {
        TODO()
    }
    suspend fun createProject(creatorPhone : String, project : Project) : Result<Project> {
        TODO()
    }
    suspend fun updateProject(userPhone: String, project : Project) : Result<Project> {
        TODO()
    }
    suspend fun getProject(userPhone : String) : Result<Project> {
        TODO()
    }
}