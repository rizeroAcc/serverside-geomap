package com.mapprjct.database.storage

import arrow.core.Either
import com.mapprjct.com.mapprjct.exceptions.storage.DeletePlacemarkIconError
import com.mapprjct.com.mapprjct.exceptions.storage.GetPlacemarkIconFileError
import com.mapprjct.com.mapprjct.exceptions.storage.MoveIconError
import com.mapprjct.com.mapprjct.exceptions.storage.WritePlacemarkIconError
import com.mapprjct.exceptions.storage.SaveOrUpdatePlacemarkIconError
import com.mapprjct.model.dto.PlacemarkDTO
import io.ktor.utils.io.ByteReadChannel
import java.io.File

interface PlacemarkIconStorage {

    suspend fun saveIcon(
        placemarkDTO: PlacemarkDTO,
        fileExtension: String,
        iconBytesProvider: suspend () -> ByteReadChannel
    ): Either<SaveOrUpdatePlacemarkIconError, String>
    suspend fun getPlacemarkIcon(placemarkDTO: PlacemarkDTO): Either<GetPlacemarkIconFileError, ByteReadChannel>
    suspend fun deletePlacemarkIcon(placemarkDTO: PlacemarkDTO): Either<DeletePlacemarkIconError, Unit>
}