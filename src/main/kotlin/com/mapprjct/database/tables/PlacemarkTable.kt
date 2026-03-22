package com.mapprjct.database.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.charLength
import java.util.UUID

object PlacemarkTable : Table("placemark") {
    val id = uuid("placemark_id").uniqueIndex().clientDefault { UUID.randomUUID() }
    val project = reference("project_id", ProjectTable.id)
    val name = varchar("name", 80)
    val latitude = double("latitude")
    val longitude = double("longitude")
    val address = varchar("address", 255).nullable()
    val icon = text("icon").nullable() //Путь к файлу на сервере
    val versionID = uuid("version_id").clientDefault { UUID.randomUUID() }
}