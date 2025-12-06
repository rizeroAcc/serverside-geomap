package com.mapprjct.database.sessions

import kotlinx.serialization.Serializable

@Serializable
data class APISession(val phone : String,val expireAt : Long)