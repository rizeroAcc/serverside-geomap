package com.mapprjct.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProjectWithRole(
    val project: Project,
    val role: Int,
)
