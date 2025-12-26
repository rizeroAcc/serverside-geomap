package com.mapprjct.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val projectID : String,
    val name : String,
    val membersCount : Int,
)
