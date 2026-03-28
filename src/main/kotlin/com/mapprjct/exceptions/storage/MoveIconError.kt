package com.mapprjct.com.mapprjct.exceptions.storage

import kotlinx.io.IOException

sealed interface MoveIconError{
    data object UnsupportedFileOperations : MoveIconError
    data class IO(val cause : IOException): MoveIconError
    data class Unexpected(val cause: Throwable): MoveIconError
}
