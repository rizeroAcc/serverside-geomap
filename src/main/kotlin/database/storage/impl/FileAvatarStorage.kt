package com.mapprjct.database.storage.impl


import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.fx.coroutines.resourceScope
import com.mapprjct.AppConfig
import com.mapprjct.database.storage.AvatarStorage
import com.mapprjct.exceptions.storage.UpdateAvatarFileError
import com.mapprjct.model.dto.User
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

    val uploadDir = getOrCreateUploadDirectory(appConfig.avatarResourcePath)

    override suspend fun saveOrReplaceUserAvatar(
        user: User,
        fileExtension: String,
        avatarByteProvider: suspend () -> ByteReadChannel
    ): Either<UpdateAvatarFileError, String> = either {
        val tempFile = File(uploadDir, "${user.phone}.tmp.${UUID.randomUUID()}")
        val newFileName = "${user.phone}.$fileExtension"
        val finalFile = File(uploadDir, newFileName)

        // resourceScope гарантирует выполнение release-блока (удаление tempFile)
        resourceScope {
            install({ tempFile }) { file, _ -> file.delete() }

            catch({
                withContext(Dispatchers.IO) {
                    // Копируем данные во временный файл
                    avatarByteProvider().copyAndClose(tempFile.writeChannel())
                }

                // Удаляем старый аватар (если есть)
                user.avatarFilename?.let { oldName ->
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

    override suspend fun getUserAvatar(avatarFilename: String): Result<File> {
        return runCatching {
            val avatar = File(uploadDir, avatarFilename)
            if (!avatar.exists()) throw FileNotFoundException()
            avatar
        }
    }

    override suspend fun deleteAvatar(avatarFilename: String): Result<Unit> {
        val avatarFile = File(uploadDir, avatarFilename)
        return runCatching {
            if (!avatarFile.exists()) throw FileNotFoundException()
            if (!avatarFile.delete()) throw IOException()
        }
    }

    override suspend fun getUploadDirectory(): File {
        return uploadDir
    }

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

    /**
     * @throws IllegalStateException - if failed to create directory
     * */
    private fun getOrCreateUploadDirectory(relatePath : String) : File {
        val directory = File(relatePath)
        if (!directory.exists()) {
            if(!directory.mkdirs()){
                throw IllegalStateException("Could not create directory ${directory.absolutePath}")
            }
        }
        return directory
    }


}