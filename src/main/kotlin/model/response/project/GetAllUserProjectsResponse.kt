package com.mapprjct.model.response.project

import com.mapprjct.model.dto.ProjectWithRole
import kotlinx.serialization.Serializable

@Serializable
data class GetAllUserProjectsResponse(
    val result: List<ProjectWithRole>,
)