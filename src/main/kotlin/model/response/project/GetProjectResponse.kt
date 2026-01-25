package com.mapprjct.model.response.project

import com.mapprjct.model.dto.Project
import kotlinx.serialization.Serializable

@Serializable
data class GetProjectResponse(
    val project : Project,
)
