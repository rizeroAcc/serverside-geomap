package com.mapprjct.database.repositoryImpl

import com.mapprjct.database.repository.PlacemarkRepository
import com.mapprjct.database.tables.PlacemarkPhotoTable
import com.mapprjct.database.tables.PlacemarkTable
import com.mapprjct.model.datatype.Latitude
import com.mapprjct.model.datatype.Longitude
import com.mapprjct.model.datatype.StringUUID
import com.mapprjct.model.dto.PlacemarkDTO
import com.mapprjct.model.dto.PlacemarkPhotoDTO
import com.mapprjct.utils.toStringUUID
import com.mapprjct.utils.toUUID
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.updateReturning
import java.util.UUID

class PlacemarkRepositoryImpl : PlacemarkRepository {
    override suspend fun findByID(placemarkID : StringUUID): PlacemarkDTO? {
        return PlacemarkTable.selectAll().where {
                PlacemarkTable.id eq placemarkID.toUUID()
            }.singleOrNull()?.toPlacemark()
    }

    override suspend fun findAllProjectPlacemarks(projectID : StringUUID): List<PlacemarkDTO> {
        return PlacemarkTable.selectAll()
            .where{ PlacemarkTable.project eq projectID.toUUID() }
            .map { it.toPlacemark() }
    }

    override suspend fun insert(placemark: PlacemarkDTO) : PlacemarkDTO {
        return PlacemarkTable.insertReturning(
            returning = PlacemarkTable.columns
        ) {
            it[PlacemarkTable.id] = UUID.randomUUID()
            it[PlacemarkTable.project] = placemark.projectID.toUUID()
            it[PlacemarkTable.name] = placemark.name
            it[PlacemarkTable.latitude] = placemark.latitude.value
            it[PlacemarkTable.longitude] = placemark.longitude.value
            it[PlacemarkTable.address] = placemark.address
            it[PlacemarkTable.icon] = placemark.icon
            it[PlacemarkTable.versionID] = UUID.randomUUID()
        }
            .single()
            .toPlacemark()
    }

    override suspend fun update(updatedPlacemark: PlacemarkDTO) : PlacemarkDTO? {
        return PlacemarkTable.updateReturning(
            where = {
                PlacemarkTable.id eq updatedPlacemark.placemarkID.toUUID() and
                        (PlacemarkTable.versionID eq updatedPlacemark.versionID.toUUID())
            },
            returning = PlacemarkTable.columns
        ) {
            it[PlacemarkTable.name] = updatedPlacemark.name
            it[PlacemarkTable.latitude] = updatedPlacemark.latitude.value
            it[PlacemarkTable.longitude] = updatedPlacemark.longitude.value
            it[PlacemarkTable.address] = updatedPlacemark.address
            it[PlacemarkTable.icon] = updatedPlacemark.icon
            it[PlacemarkTable.versionID] = UUID.randomUUID()
        }
            .singleOrNull()?.toPlacemark()
    }

    override suspend fun findAllPlacemarkPhotos(placemarkId: StringUUID): List<PlacemarkPhotoDTO> {
        return PlacemarkPhotoTable.selectAll().where {
            PlacemarkPhotoTable.placemarkID eq placemarkId.toUUID()
        }.map {
            PlacemarkPhotoDTO(
                id = it[PlacemarkPhotoTable.id].toStringUUID(),
                placemarkID = placemarkId,
                photo = it[PlacemarkPhotoTable.photo]
            )
        }
    }
    private fun ResultRow.toPlacemark() : PlacemarkDTO {
        return PlacemarkDTO(
            placemarkID = this[PlacemarkTable.id].toStringUUID(),
            projectID = this[PlacemarkTable.project].toStringUUID(),
            name = this[PlacemarkTable.name],
            latitude = Latitude(this[PlacemarkTable.latitude]),
            longitude = Longitude(this[PlacemarkTable.longitude]),
            address = this[PlacemarkTable.address],
            icon = this[PlacemarkTable.icon],
            versionID = this[PlacemarkTable.versionID].toStringUUID()
        )
    }
}