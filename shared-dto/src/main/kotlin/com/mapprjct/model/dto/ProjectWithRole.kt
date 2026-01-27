package com.mapprjct.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProjectWithRole(
    val project: com.mapprjct.model.dto.Project,
    val role: Int,
)
