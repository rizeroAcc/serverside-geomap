package com.mapprjct.exceptions.storage

sealed interface UpdateAvatarFileError {
    data object FilesystemError : UpdateAvatarFileError
    data object ConnectionTerminated : UpdateAvatarFileError
    data class Unexpected(val cause: Throwable) : UpdateAvatarFileError
}