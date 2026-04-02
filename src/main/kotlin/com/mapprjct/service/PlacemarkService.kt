package com.mapprjct.service

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.mapprjct.com.mapprjct.exceptions.domain.placemark.DeletePlacemarkError
import com.mapprjct.com.mapprjct.exceptions.domain.placemark.GetAllProjectPlacemarksError
import com.mapprjct.com.mapprjct.exceptions.domain.placemark.GetPlacemarkError
import com.mapprjct.com.mapprjct.exceptions.domain.placemark.UpdatePlacemarkError
import com.mapprjct.com.mapprjct.utils.TransactionProvider
import com.mapprjct.database.repository.PlacemarkRepository
import com.mapprjct.database.repository.ProjectRepository
import com.mapprjct.database.storage.PlacemarkIconStorage
import com.mapprjct.exceptions.domain.placemark.CreatePlacemarkError
import com.mapprjct.exceptions.domain.placemark.UpdatePlacemarkIconError
import com.mapprjct.model.datatype.Role
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.StringUUID
import com.mapprjct.model.dto.PlacemarkDTO
import com.mapprjct.utils.toUUID
import io.ktor.utils.io.*
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

class PlacemarkService(
    val transactionProvider: TransactionProvider,
    val placemarkRepository: PlacemarkRepository,
    val projectRepository: ProjectRepository,
    val placemarkIconStorage: PlacemarkIconStorage,
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

    suspend fun getPlacemark(placemarkID : StringUUID) : Either<GetPlacemarkError, PlacemarkDTO> = either {
        catch({
            placemarkRepository.findByID(placemarkID) ?: raise(GetPlacemarkError.NotFound(placemarkID.value))
        }){ error->
            when(error){
                is ExposedSQLException -> raise(GetPlacemarkError.Database(error))
                else -> raise(GetPlacemarkError.Unexpected(error))
            }
        }
    }
    suspend fun getAllProjectPlacemarks(userPhone : RussiaPhoneNumber, projectID : StringUUID) : Either<GetAllProjectPlacemarksError, List<PlacemarkDTO>> = either {
        catch({
            transactionProvider.runInTransaction{
                ensureNotNull(projectRepository.getProjectById(projectID.toUUID())){
                    GetAllProjectPlacemarksError.ProjectNotFound(projectID.value)
                }
                ensureNotNull(projectRepository.findUserMembershipInProject(userPhone, projectID.toUUID())){
                    GetAllProjectPlacemarksError.UserNotStayInProject(projectID.value)
                }
                placemarkRepository.findAllProjectPlacemarks(projectID)
            }
        }){ error->
            when(error){
                is ExposedSQLException -> raise(GetAllProjectPlacemarksError.Database(error))
                else -> raise(GetAllProjectPlacemarksError.Unexpected(error))
            }
        }
    }

    suspend fun updatePlacemark(
        userPhone: RussiaPhoneNumber,
        updatablePlacemarkID : StringUUID,
        updatedPlacemarkDTO: PlacemarkDTO
    ) : Either<UpdatePlacemarkError, PlacemarkDTO> = either {
        ensure(updatablePlacemarkID == updatedPlacemarkDTO.placemarkID){
            UpdatePlacemarkError.IDUpdateForbidden
        }
        ensure(updatedPlacemarkDTO.name.isNotBlank()){
            UpdatePlacemarkError.BlankName
        }
        catch({
            transactionProvider.runInTransaction {
                val oldPlacemark = placemarkRepository.findByID(updatablePlacemarkID) ?: raise(UpdatePlacemarkError.NotFound(updatablePlacemarkID.value))
                ensure(oldPlacemark.projectID == updatedPlacemarkDTO.projectID){
                    UpdatePlacemarkError.ProjectIDUpdateForbidden
                }
                val membership = ensureNotNull(projectRepository.findUserMembershipInProject(userPhone,oldPlacemark.projectID.toUUID())){
                    UpdatePlacemarkError.UserNotStayInProject(oldPlacemark.projectID.value)
                }
                ensure(membership.role != Role.Worker){
                    UpdatePlacemarkError.NoPermissionToUpdatePlacemark(oldPlacemark.projectID.value)
                }
                ensure(oldPlacemark.versionID == updatedPlacemarkDTO.versionID){
                    UpdatePlacemarkError.VersionConflict(oldPlacemark)
                }
                placemarkRepository.update(updatedPlacemarkDTO) ?: raise(UpdatePlacemarkError.ConcurrentUpdate)
            }
        }){ error->
            when(error){
                is ExposedSQLException -> raise(UpdatePlacemarkError.Database(error))
                else -> raise(UpdatePlacemarkError.Unexpected(error))
            }
        }
    }
    suspend fun updatePlacemarkIcon(
        userPhone : RussiaPhoneNumber,
        clientPlacemark : PlacemarkDTO,
        fileName : String,
        fileDataChannelProvider : suspend ()-> ByteReadChannel
    ) : Either<UpdatePlacemarkIconError, PlacemarkDTO> = either {

        val oldPlacemark = placemarkRepository.findByID(clientPlacemark.placemarkID) ?: raise(UpdatePlacemarkIconError.NotFound(clientPlacemark.placemarkID.value))

        val membership = ensureNotNull(projectRepository.findUserMembershipInProject(userPhone,oldPlacemark.projectID.toUUID())){
            UpdatePlacemarkIconError.UserNotStayInProject(oldPlacemark.projectID.value)
        }
        ensure(membership.role != Role.Worker){
            UpdatePlacemarkIconError.NoPermissionToUpdatePlacemark(oldPlacemark.projectID.value)
        }
        ensure(oldPlacemark.versionID == clientPlacemark.versionID){
            UpdatePlacemarkIconError.VersionConflict(oldPlacemark)
        }

        val allowedExtensions = getAllowedAvatarFormats()
        val extension = "." + fileName.substringAfterLast('.').lowercase()
        ensure(extension in allowedExtensions) {
            UpdatePlacemarkIconError.InvalidIconFormat(allowedExtensions)
        }

        TODO()
    }


    suspend fun deletePlacemark() : Either<DeletePlacemarkError, Unit> = either {
        TODO()
    }

    fun getAllowedAvatarFormats() = listOf(".jpg", ".jpeg", ".png")
}