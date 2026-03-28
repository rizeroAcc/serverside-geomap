package com.mapprjct.com.mapprjct.exceptions.storage

import java.io.IOException

sealed interface RecoverFromBackupError {
    data class Unexpected(val cause : Throwable) : RecoverFromBackupError
    data object EmptyOldFilename : RecoverFromBackupError
    data object BackupFileCorrupted : RecoverFromBackupError
    data class Filesystem(val cause : IOException) : RecoverFromBackupError
}
