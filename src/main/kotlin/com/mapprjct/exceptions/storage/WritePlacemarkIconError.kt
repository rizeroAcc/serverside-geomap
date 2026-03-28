package com.mapprjct.com.mapprjct.exceptions.storage

import kotlinx.io.IOException

sealed interface WritePlacemarkIconError {
    data class Unexpected(val cause: Throwable) : WritePlacemarkIconError
    data object UnsupportedFileOperations : WritePlacemarkIconError
    data object IconAlreadyHaveIcon : WritePlacemarkIconError
    data class IO(val cause: IOException) : WritePlacemarkIconError
}
