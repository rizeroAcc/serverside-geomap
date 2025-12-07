package com.mapprjct.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateProjectRequest(
    val projectName: String,
)