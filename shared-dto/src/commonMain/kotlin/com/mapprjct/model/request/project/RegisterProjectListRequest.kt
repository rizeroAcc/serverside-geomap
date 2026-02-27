package com.mapprjct.model.request.project

import com.mapprjct.model.dto.UnregisteredProject
import kotlinx.serialization.Serializable

@Serializable
data class RegisterProjectListRequest(
    val projects: List<UnregisteredProject>
)
