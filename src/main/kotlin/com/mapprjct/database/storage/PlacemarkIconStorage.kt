package com.mapprjct.database.storage

import arrow.core.Either
import aws.smithy.kotlin.runtime.content.ByteStream
import com.mapprjct.com.mapprjct.exceptions.storage.GetPlacemarkIconError
import com.mapprjct.exceptions.storage.SavePlacemarkIconError
import io.ktor.utils.io.ByteReadChannel

interface PlacemarkIconStorage {

    suspend fun saveIcon(
        iconKey: String,
        iconBytesProvider: suspend () -> ByteReadChannel
    ): Either<SavePlacemarkIconError, String>
    suspend fun getIcon(iconKey : String): Either<GetPlacemarkIconError, ByteStream>
}