package com.mapprjct.database.dao

import com.mapprjct.database.projects.ProjectDTO
import com.mapprjct.database.users.UserDTO

interface ProjectDAO {
    suspend fun getAllUserProjects(userDTO: UserDTO) : List<ProjectDTO>
}