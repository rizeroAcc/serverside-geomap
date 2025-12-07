package com.mapprjct.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProjectDTO(
    val projectId:String,
    val name:String,
)