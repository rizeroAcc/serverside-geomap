package com.mapprjct.dto

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val phone : String,
    val username : String = "",
    val avatar : String? = null,
)