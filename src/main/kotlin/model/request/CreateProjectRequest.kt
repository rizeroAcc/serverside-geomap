package com.mapprjct.model.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateProjectRequest(
    val projectName: String,
)