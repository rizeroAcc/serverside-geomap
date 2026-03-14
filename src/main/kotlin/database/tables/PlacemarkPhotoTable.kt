package com.mapprjct.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object PlacemarkPhotoTable : Table("placemark_photo") {
    val id = uuid("placemark_photo_id").uniqueIndex()
    val placemarkID = reference("placemark_id", PlacemarkTable.id, onDelete = ReferenceOption.CASCADE)
    val photo = text("photo")

    override val primaryKey = PrimaryKey(id, name = "PK_placemark_photo_ID")
    init {
        uniqueIndex(placemarkID,id)
    }
}