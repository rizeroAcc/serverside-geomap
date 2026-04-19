package com.mapprjct.database.repository

import com.mapprjct.model.datatype.StringUUID
import com.mapprjct.model.dto.PlacemarkDTO
import com.mapprjct.model.dto.PlacemarkPhotoDTO

interface PlacemarkRepository {
    suspend fun findByID(placemarkID : StringUUID) : PlacemarkDTO?
    suspend fun findAllProjectPlacemarks(projectID : StringUUID) : List<PlacemarkDTO>
    suspend fun insert(placemark : PlacemarkDTO) : PlacemarkDTO
    suspend fun update(updatedPlacemark : PlacemarkDTO) : PlacemarkDTO?
    suspend fun delete(placemark : PlacemarkDTO) : Int
    suspend fun findAllPlacemarkPhotos(placemarkId : StringUUID) : List<PlacemarkPhotoDTO>
}