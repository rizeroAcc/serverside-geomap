package com.mapprjct.service

import com.mapprjct.database.repository.PlacemarkRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.exceptions.domain.placemark.CreatePlacemarkError
import com.mapprjct.model.datatype.Role
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.dto.PlacemarkDTO
import com.mapprjct.utils.Either
import com.mapprjct.utils.toEither
import com.mapprjct.utils.toUUID
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

class PlacemarkService(
    val database: Database,
    val placemarkRepository: PlacemarkRepository,
    val projectRepository: ProjectRepository,
) {
    suspend fun createPlacemark(placemarkDTO: PlacemarkDTO, userPhone : RussiaPhoneNumber) : Either<PlacemarkDTO, CreatePlacemarkError> {
        //check project exists
        //check user stay in project and have permissions to create placemarks
        return runCatching {
            if (placemarkDTO.name.isBlank()){
                throw CreatePlacemarkError.EmptyProjectName()
            }
            if (placemarkDTO.latitude <= 0.0 || placemarkDTO.latitude > 90.0){
                throw CreatePlacemarkError.InvalidLatitude()
            }
            if (placemarkDTO.longitude <= 0.0 || placemarkDTO.longitude > 180.0){
                throw CreatePlacemarkError.InvalidLongitude()
            }
            suspendTransaction(database) {
                projectRepository.getProjectById(placemarkDTO.projectID.toUUID())
                    ?: throw CreatePlacemarkError.ProjectNotFound(placemarkDTO.projectID.value)
                val membership = projectRepository.findUserMembershipInProject(userPhone,placemarkDTO.projectID.toUUID())
                    ?: throw CreatePlacemarkError.UserNotStayInProject(placemarkDTO.projectID.value)
                if (membership.role == Role.Worker){
                    throw CreatePlacemarkError.NoPermissionToCreatePlacemark(placemarkDTO.projectID.value)
                }
                return@suspendTransaction placemarkRepository.insert(placemarkDTO)

            }
        }.toEither { error->
            when(error){
                is ExposedSQLException -> CreatePlacemarkError.Database (error)
                else -> CreatePlacemarkError.Unexpected(error)
            }
        }
    }
}