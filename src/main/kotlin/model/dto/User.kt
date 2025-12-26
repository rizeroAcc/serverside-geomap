package com.mapprjct.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val phone : String,
    val username : String,
    val avatarPath : String? = null,
)