package com.mapprjct.database.dao

import com.mapprjct.dto.Project
import com.mapprjct.dto.User

interface ProjectDAO {
    suspend fun getAllUserProjects(user: User) : List<Project>
    suspend fun createProject(creatorPhone : String, projectName: String) : Project?
}