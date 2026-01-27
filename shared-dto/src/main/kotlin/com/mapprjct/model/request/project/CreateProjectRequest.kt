package com.mapprjct.model.request.project

import kotlinx.serialization.Serializable

@Serializable
data class CreateProjectRequest(
    val projectName: String,
)