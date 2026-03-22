package com.mapprjct.model.dto

import com.mapprjct.model.datatype.StringUUID
import kotlinx.serialization.Serializable

@Serializable
data class ProjectDTO(
    val projectID : StringUUID,
    val name : String,
    val membersCount : Int,
)
