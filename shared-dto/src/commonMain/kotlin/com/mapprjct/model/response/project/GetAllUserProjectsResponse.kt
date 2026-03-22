package com.mapprjct.model.response.project

import kotlinx.serialization.Serializable

@Serializable
data class GetAllUserProjectsResponse(
    val result: List<com.mapprjct.model.dto.ProjectMembershipDTO>,
)