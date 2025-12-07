package com.mapprjct.database.dao

import com.mapprjct.dto.ProjectDTO
import com.mapprjct.dto.User

interface ProjectDAO {
    suspend fun getAllUserProjects(user: User) : List<ProjectDTO>
    suspend fun createProject(creatorPhone : String, projectDTO: ProjectDTO) : ProjectDTO?
}