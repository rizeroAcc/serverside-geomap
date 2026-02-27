package com.mapprjct.model.response.project

import com.mapprjct.model.datatype.StringUUID
import com.mapprjct.model.dto.Project
import com.mapprjct.model.dto.ProjectRegistrationResult
import kotlinx.serialization.Serializable

/**
 * Represents registered projects list where first is [Project], and second old project ID (if was sent)
 * */
@Serializable
data class RegisterProjectListResponse(
    val registeredProjects: List<ProjectRegistrationResult>
)
