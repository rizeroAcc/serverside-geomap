package com.mapprjct.dto

import kotlinx.serialization.Serializable

@Serializable
data class APISession(val phone : String,val expireAt : Long)