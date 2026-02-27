package com.mapprjct.model.dto

import com.mapprjct.model.datatype.StringUUID
import kotlinx.serialization.Serializable

@Serializable
data class UnregisteredProject(
    val oldID : StringUUID? = null,
    val name : String,
)
