package com.mapprjct.model.dto

import com.mapprjct.model.datatype.StringUUID
import kotlinx.serialization.Serializable

@Serializable
data class ProjectRegistrationResult(
    val project: Project,
    val oldID : StringUUID?
)
