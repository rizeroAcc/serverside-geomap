package com.mapprjct.model.dto

import com.mapprjct.model.datatype.StringUUID
import kotlinx.serialization.Serializable

@Serializable
data class ProjectRegistrationResultDTO(
    val projectDTO: ProjectDTO,
    val oldID : StringUUID?
)
