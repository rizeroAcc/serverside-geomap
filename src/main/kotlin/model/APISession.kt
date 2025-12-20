package com.mapprjct.model

import kotlinx.serialization.Serializable

@Serializable
data class APISession(val phone : String,val expireAt : Long)