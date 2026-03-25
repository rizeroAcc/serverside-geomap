package com.mapprjct.database.storage

import arrow.core.Either
import com.mapprjct.exceptions.storage.SaveOrUpdatePlacemarkIconError
import kotlinx.io.IOException
import kotlinx.io.files.FileNotFoundException
import java.io.File

interface PlacemarkIconStorage {
    suspend fun saveOrUpdateIcon() : Either<SaveOrUpdatePlacemarkIconError, String>?
    suspend fun getPlacemarkIcon() : Either<FileNotFoundException,File>
    suspend fun deletePlacemarkIcon() : Either<IOException,Unit>
    suspend fun getUploadDirectory() : File
}