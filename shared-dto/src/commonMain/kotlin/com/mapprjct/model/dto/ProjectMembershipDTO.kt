package com.mapprjct.model.dto

import com.mapprjct.model.datatype.Role
import kotlinx.serialization.Serializable

@Serializable
data class ProjectMembershipDTO(
    val projectDTO: ProjectDTO,
    val role: Role,
)
