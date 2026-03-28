//package com.mapprjct.database.storage.impl
//
//import arrow.core.Either
//import arrow.core.raise.catch
//import arrow.core.raise.either
//import arrow.core.raise.ensure
//import arrow.core.raise.ensureNotNull
//import arrow.fx.coroutines.resourceScope
//import com.mapprjct.AppConfig
//import com.mapprjct.com.mapprjct.exceptions.storage.DeletePlacemarkIconError
//import com.mapprjct.com.mapprjct.exceptions.storage.GetPlacemarkIconFileError
//import com.mapprjct.com.mapprjct.exceptions.storage.MoveIconError
//import com.mapprjct.com.mapprjct.exceptions.storage.WritePlacemarkIconError
//import com.mapprjct.database.storage.PlacemarkIconStorage
//import com.mapprjct.model.dto.PlacemarkDTO
//import com.mapprjct.utils.getOrCreateDirectory
//import io.ktor.util.cio.*
//import io.ktor.utils.io.*
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import kotlinx.io.IOException
//import java.io.File
//import java.nio.file.AtomicMoveNotSupportedException
//import java.nio.file.Files
//import java.nio.file.StandardCopyOption
//import java.util.*
//
//class FilePlacemarkIconStorage(
//    appConfig: AppConfig
//) : PlacemarkIconStorage {
//    val uploadDir = getOrCreateDirectory(appConfig.placemarkIconsPath)
//    override fun calculateNewIconFilenameForPlacemark(placemarkDTO: PlacemarkDTO, fileExtension: String): String {
//        return "placemark_${placemarkDTO.placemarkID.value}_icon.$fileExtension"
//    }
//
//    override suspend fun write(
//        iconBytesProvider: suspend () -> ByteReadChannel,
//        filename: String
//    ) : Either<WritePlacemarkIconError,File> = either{
//        val tempFile = File(uploadDir, filename.substringBeforeLast('.') + ".tmp.${UUID.randomUUID()}")
//        resourceScope {
//            install({tempFile}) { tempFile,_ -> if (tempFile.exists()) tempFile.delete() }
//            catch({
//                withContext(Dispatchers.IO) {
//                    iconBytesProvider().copyAndClose(tempFile.writeChannel())
//                }
//                val iconFile = File(uploadDir, filename)
//                Files.move(tempFile.toPath(), iconFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
//                iconFile
//            }){ error->
//                when(error){
//                    is UnsupportedOperationException, is AtomicMoveNotSupportedException -> raise(WritePlacemarkIconError.UnsupportedFileOperations)
//                    is FileAlreadyExistsException -> raise(WritePlacemarkIconError.IconAlreadyHaveIcon)
//                    is IOException -> raise(WritePlacemarkIconError.IO(error))
//                    else -> raise(WritePlacemarkIconError.Unexpected(error))
//                }
//            }
//        }
//    }
//
//    override suspend fun getPlacemarkIcon(placemarkDTO: PlacemarkDTO): Either<GetPlacemarkIconFileError, File> = either {
//        ensureNotNull(placemarkDTO.icon){
//            GetPlacemarkIconFileError.PlacemarkDoesNotHaveIcon
//        }
//        val icon = File(uploadDir,placemarkDTO.icon!!)
//        ensure(icon.isFile && icon.exists()){ GetPlacemarkIconFileError.FileCorrupted }
//        icon
//    }
//
//    override suspend fun deletePlacemarkIcon(placemarkDTO: PlacemarkDTO): Either<DeletePlacemarkIconError, Unit> = either {
//        ensureNotNull(placemarkDTO.icon){
//            DeletePlacemarkIconError.PlacemarkDoesNotHaveIcon
//        }
//        val iconFile = File(uploadDir,placemarkDTO.icon!!)
//        if (iconFile.exists()) {
//            catch({
//                Files.delete(iconFile.toPath())
//            }) { e: IOException ->
//                raise(DeletePlacemarkIconError.IO(e))
//            }
//        }
//    }
//
//    override suspend fun moveIcon(
//        oldFile: File,
//        newFile: File
//    ): Either<MoveIconError, File> = either {
//        catch({
//            val newPath = Files.move(oldFile.toPath(), newFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
//            newPath.toFile()
//        }){ error->
//            when(error){
//                is UnsupportedOperationException, is AtomicMoveNotSupportedException -> raise(MoveIconError.UnsupportedFileOperations)
//                is IOException -> raise(MoveIconError.IO(error))
//                else -> raise(MoveIconError.Unexpected(error))
//            }
//        }
//    }
//
//    override suspend fun getUploadDirectory(): File = uploadDir
//}