package com.mapprjct.database.dao

import com.mapprjct.dto.Project
import com.mapprjct.dto.Role

interface ProjectDAO {
    suspend fun getAllUserProjects(userPhone: String) : List<Pair<Project, Role>>
    suspend fun createProject(creatorPhone : String, projectName: String) : Project?
}