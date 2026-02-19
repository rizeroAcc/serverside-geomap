package com.mapprjct.model.dto

import com.mapprjct.model.datatype.Role
import kotlinx.serialization.Serializable

@Serializable
data class ProjectMembership(
    val project: Project,
    val role: Role,
)
