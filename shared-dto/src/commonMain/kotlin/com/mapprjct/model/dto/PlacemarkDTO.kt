package com.mapprjct.model.dto

import com.mapprjct.model.datatype.StringUUID
import kotlinx.serialization.Serializable

@Serializable
data class PlacemarkDTO(
    val placemarkID : StringUUID,
    val projectID : StringUUID,
    val name : String,
    val latitude : Double,
    val longitude : Double,
    val address : String? = null,
    val icon : String? = null,
    val versionID : StringUUID,
)
