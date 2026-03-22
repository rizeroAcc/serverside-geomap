package com.mapprjct.model.response.project

import com.mapprjct.model.dto.ProjectDTO
import com.mapprjct.model.dto.ProjectRegistrationResultDTO
import kotlinx.serialization.Serializable

/**
 * Represents registered projects list where first is [ProjectDTO], and second old project ID (if was sent)
 * */
@Serializable
data class RegisterProjectListResponse(
    val registeredProjects: List<ProjectRegistrationResultDTO>
)
