package com.mapprjct.service

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.mapprjct.database.repository.PlacemarkRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.exceptions.domain.placemark.CreatePlacemarkError
import com.mapprjct.model.datatype.Role
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.dto.PlacemarkDTO
import com.mapprjct.utils.toUUID
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

class PlacemarkService(
    val database: Database,
    val placemarkRepository: PlacemarkRepository,
    val projectRepository: ProjectRepository,
) {
    suspend fun createPlacemark(placemarkDTO: PlacemarkDTO, userPhone : RussiaPhoneNumber) : Either<CreatePlacemarkError, PlacemarkDTO> = either {
        ensure(placemarkDTO.name.isNotBlank()) {
            CreatePlacemarkError.EmptyProjectName()
        }
        catch({
            suspendTransaction(database) {
                ensureNotNull(projectRepository.getProjectById(placemarkDTO.projectID.toUUID())){
                    CreatePlacemarkError.ProjectNotFound(placemarkDTO.projectID.value)
                }
                val membership = projectRepository.findUserMembershipInProject(userPhone, placemarkDTO.projectID.toUUID())
                ensureNotNull(membership){
                    CreatePlacemarkError.UserNotStayInProject(placemarkDTO.projectID.value)
                }

                ensure(membership.role != Role.Worker) {
                    CreatePlacemarkError.NoPermissionToCreatePlacemark(placemarkDTO.projectID.value)
                }
                return@suspendTransaction placemarkRepository.insert(placemarkDTO)
            }
        }) { error ->
            when (error) {
                is ExposedSQLException -> raise(CreatePlacemarkError.Database(error))
                else -> raise(CreatePlacemarkError.Unexpected(error))
            }
        }
    }
}