package com.mapprjct.model.response.project

import com.mapprjct.model.datatype.StringUUID
import com.mapprjct.model.dto.Project
import com.mapprjct.model.dto.ProjectRegistrationResult
import kotlinx.serialization.Serializable

@Serializable
data class RegisterProjectResponse(
    val registrationResult: ProjectRegistrationResult
)
