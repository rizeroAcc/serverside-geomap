package com.mapprjct.model.response.project

import com.mapprjct.model.dto.ProjectRegistrationResultDTO
import kotlinx.serialization.Serializable

@Serializable
data class RegisterProjectResponse(
    val registrationResult: ProjectRegistrationResultDTO
)
