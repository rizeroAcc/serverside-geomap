package com.mapprjct.com.mapprjct.exceptions.storage

import kotlinx.io.IOException

sealed interface DeletePlacemarkIconError {
    data object PlacemarkDoesNotHaveIcon : DeletePlacemarkIconError
    data class IO(val exception: IOException) : DeletePlacemarkIconError
}
