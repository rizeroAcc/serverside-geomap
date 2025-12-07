package com.mapprjct.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserCredentials(
    val phone : String,
    val passwordHash : String,
    val username : String,
)