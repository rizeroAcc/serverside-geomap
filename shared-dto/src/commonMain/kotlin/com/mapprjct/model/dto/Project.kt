package com.mapprjct.model.dto

import com.mapprjct.model.datatype.EntityName
import com.mapprjct.model.datatype.StringUUID
import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val projectID : StringUUID,
    val name : String,
    val membersCount : Int,
)
