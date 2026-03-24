package com.mapprjct.com.mapprjct.service

import arrow.core.Either
import arrow.core.raise.either
import com.mapprjct.com.mapprjct.exceptions.domain.placemark.AttachPhotoToPlacemarkError
import com.mapprjct.com.mapprjct.exceptions.domain.placemark.DetachAllPhotoFromPlacemarkError
import com.mapprjct.com.mapprjct.exceptions.domain.placemark.DetachPhotoFromPlacemarkError
import com.mapprjct.com.mapprjct.utils.TransactionProvider
import com.mapprjct.model.dto.PlacemarkPhotoDTO

class PlacemarkPhotoService(
    transactionProvider: TransactionProvider,
) {
    suspend fun detachAllPhotoFromPlacemark() : Either<DetachAllPhotoFromPlacemarkError, Unit> {
        TODO()
    }
    suspend fun detachPhotoFromPlacemark() : Either<DetachPhotoFromPlacemarkError, Unit> = either {
        TODO()
    }
    suspend fun attachPhotoToPlacemark() : Either<AttachPhotoToPlacemarkError, PlacemarkPhotoDTO> = either {
        TODO()
    }
}