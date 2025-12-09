package com.mapprjct.dto

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val projectID : String,
    val name : String,
)
