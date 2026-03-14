package com.mapprjct.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlacemarkWithPhotosDTO(
    val placemark : PlacemarkDTO,
    val placemarkPhotos : List<PlacemarkPhotoDTO>,
)
