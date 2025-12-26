package com.mapprjct.model.response

import com.mapprjct.model.dto.ProjectWithRole
import kotlinx.serialization.Serializable

@Serializable
data class GetAllUserProjectsResponse(
    val result: List<ProjectWithRole>,
)
