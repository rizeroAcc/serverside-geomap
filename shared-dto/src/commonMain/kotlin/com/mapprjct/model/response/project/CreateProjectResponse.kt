package com.mapprjct.model.response.project

import com.mapprjct.model.dto.Project
import kotlinx.serialization.Serializable

@Serializable
data class CreateProjectResponse(
    val project: com.mapprjct.model.dto.Project
)
