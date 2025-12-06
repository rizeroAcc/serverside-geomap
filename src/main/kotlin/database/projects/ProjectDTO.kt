package com.mapprjct.database.projects

import kotlinx.serialization.Serializable

@Serializable
data class ProjectDTO(
    val projectId:String,
    val userPhone:String,
)
