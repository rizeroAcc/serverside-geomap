package com.mapprjct.response

import com.mapprjct.dto.ProjectWithRole
import kotlinx.serialization.Serializable

@Serializable
data class GetAllUserProjectsResponse(
    val result: List<ProjectWithRole>,
)
