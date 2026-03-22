package com.mapprjct.model.request.project

import com.mapprjct.model.dto.UnregisteredProjectDTO
import kotlinx.serialization.Serializable

@Serializable
data class RegisterProjectListRequest(
    val projects: List<UnregisteredProjectDTO>
)
