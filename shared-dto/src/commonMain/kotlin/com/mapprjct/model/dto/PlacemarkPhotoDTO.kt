package com.mapprjct.model.dto

import com.mapprjct.model.datatype.StringUUID
import kotlinx.serialization.Serializable

@Serializable
data class PlacemarkPhotoDTO(
    val id : StringUUID,
    val placemarkID : StringUUID,
    val photo : String,
)
