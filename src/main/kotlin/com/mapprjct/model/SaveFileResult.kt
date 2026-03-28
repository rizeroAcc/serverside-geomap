package com.mapprjct.com.mapprjct.model

data class SaveFileResult(
    val newFilename : String,
    val backupFileName : String? = null,
)
