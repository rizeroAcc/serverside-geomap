package com.mapprjct.model.response.project

import kotlinx.serialization.Serializable

@Serializable
data class GetProjectResponse(
    val projectDTO : com.mapprjct.model.dto.ProjectDTO,
)
