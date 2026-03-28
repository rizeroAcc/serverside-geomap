package com.mapprjct.database.storage.impl


import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.fx.coroutines.resourceScope
import com.mapprjct.AppConfig
import com.mapprjct.database.storage.AvatarStorage
import com.mapprjct.exceptions.storage.UpdateAvatarFileError
import com.mapprjct.model.dto.UserDTO
import com.mapprjct.utils.getOrCreateDirectory
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID

class FileAvatarStorage(
    appConfig: AppConfig
) : AvatarStorage{
    val uploadDir : File = getOrCreateDirectory(appConfig.avatarResourcePath)

    override suspend fun saveOrReplaceUserAvatar(
        userDTO: UserDTO,
        fileExtension: String,
        avatarByteProvider: suspend () -> ByteReadChannel
    ): Either<UpdateAvatarFileError, String> = either {
        val tempFile = File(uploadDir, "${userDTO.phone}.tmp.${UUID.randomUUID()}")
        val newFileName = "${userDTO.phone}$fileExtension"
        val finalFile = File(uploadDir, newFileName)

        resourceScope {
            install({ tempFile }) { file, _ -> file.delete() }

            catch({
                withContext(Dispatchers.IO) {
                    // Копируем данные во временный файл
                    avatarByteProvider().copyAndClose(tempFile.writeChannel())
                }

                // Удаляем старый аватар (если есть)
                userDTO.avatarFilename?.let { oldName ->
                    removeOldAvatarIfExists(uploadDir, oldName)
                }

                // Переименовываем временный файл в финальный
                ensure(tempFile.renameTo(finalFile)) {
                    UpdateAvatarFileError.FilesystemError
                }

                newFileName
            }) { ex ->
                when (ex) {
                    is IOException -> raise(UpdateAvatarFileError.FilesystemError)
                    else -> raise(UpdateAvatarFileError.ConnectionTerminated)
                }
            }
        }
    }

    override suspend fun getUserAvatar(avatarFilename: String) : Either<FileNotFoundException,File> = either {
        val avatar = File(uploadDir, avatarFilename)
        if (!avatar.isFile) raise(FileNotFoundException())
        if (!avatar.exists()) raise(FileNotFoundException())
        avatar
    }

    override suspend fun deleteAvatar(avatarFilename: String): Either<IOException,Unit> = either {
        val avatarFile = File(uploadDir, avatarFilename)
        if (!avatarFile.exists()) raise(FileNotFoundException())
        if (!avatarFile.delete()) raise(IOException())
    }

    override suspend fun getUploadDirectory(): File = uploadDir

    /**
     * @throws java.io.IOException - if failed to remove old file
     * */
    private fun removeOldAvatarIfExists(directory : File, oldFilename : String?) {
        oldFilename?.let { oldFileName ->
            val oldFile = File(directory, oldFileName)
            if (oldFile.exists()) {
                if (!oldFile.delete()){
                    throw IOException("Failed to delete file ${oldFile.path}")
                }
            }
        }
    }
}