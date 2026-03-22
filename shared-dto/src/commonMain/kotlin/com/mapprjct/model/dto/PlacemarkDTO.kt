package com.mapprjct.model.dto

import com.mapprjct.model.datatype.Latitude
import com.mapprjct.model.datatype.Longitude
import com.mapprjct.model.datatype.StringUUID
import kotlinx.serialization.Serializable

@Serializable
data class PlacemarkDTO(
    val placemarkID : StringUUID,
    val projectID : StringUUID,
    val name : String,
    val latitude : Latitude,
    val longitude : Longitude,
    val address : String? = null,
    val icon : String? = null,
    val versionID : StringUUID,
)
