package com.mapprjct.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserCredentials(
    val phone : String,
    val password : String,
)