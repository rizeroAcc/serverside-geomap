package com.mapprjct.service

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.mapprjct.com.mapprjct.exceptions.domain.placemark.AttachPhotoToPlacemarkError
import com.mapprjct.com.mapprjct.exceptions.domain.placemark.DeletePlacemarkError
import com.mapprjct.com.mapprjct.exceptions.domain.placemark.DetachAllPhotoFromPlacemarkError
import com.mapprjct.com.mapprjct.exceptions.domain.placemark.DetachPhotoFromPlacemarkError
import com.mapprjct.com.mapprjct.exceptions.domain.placemark.GetAllProjectPlacemarksError
import com.mapprjct.com.mapprjct.exceptions.domain.placemark.GetPlacemarkError
import com.mapprjct.com.mapprjct.exceptions.domain.placemark.UpdatePlacemarkError
import com.mapprjct.com.mapprjct.exceptions.domain.placemark.UpdatePlacemarkIconError
import com.mapprjct.com.mapprjct.service.PlacemarkPhotoService
import com.mapprjct.com.mapprjct.utils.TransactionProvider
import com.mapprjct.database.repository.PlacemarkRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.exceptions.domain.placemark.CreatePlacemarkError
import com.mapprjct.model.datatype.Role
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.dto.PlacemarkDTO
import com.mapprjct.model.dto.PlacemarkPhotoDTO
import com.mapprjct.utils.toUUID
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

class PlacemarkService(
    val placemarkPhotoService: PlacemarkPhotoService,

    val transactionProvider: TransactionProvider,
    val placemarkRepository: PlacemarkRepository,
    val projectRepository: ProjectRepository,
) {
    suspend fun createPlacemark(placemarkDTO: PlacemarkDTO, userPhone : RussiaPhoneNumber) : Either<CreatePlacemarkError, PlacemarkDTO> = either {
        ensure(placemarkDTO.name.isNotBlank()) {
            CreatePlacemarkError.EmptyName
        }
        catch({
            transactionProvider.runInTransaction{
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
                placemarkRepository.insert(placemarkDTO)
            }
        }) { error ->
            when (error) {
                is ExposedSQLException -> raise(CreatePlacemarkError.Database(error))
                else -> raise(CreatePlacemarkError.Unexpected(error))
            }
        }
    }

    suspend fun getPlacemark() : Either<GetPlacemarkError, PlacemarkDTO> = either {
        TODO()
    }
    suspend fun getAllProjectPlacemarks() : Either<GetAllProjectPlacemarksError,Unit> = either {
        TODO()
    }

    suspend fun updatePlacemarkIcon() : Either<UpdatePlacemarkIconError, PlacemarkDTO> = either {
        TODO()
    }
    suspend fun updatePlacemark() : Either<UpdatePlacemarkError, PlacemarkDTO> = either {
        TODO()
    }

    suspend fun deletePlacemark() : Either<DeletePlacemarkError, Unit> = either {
        TODO()
    }
}