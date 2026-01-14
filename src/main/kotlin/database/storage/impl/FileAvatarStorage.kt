package com.mapprjct.database.storage.impl

import com.mapprjct.AppConfig
import com.mapprjct.database.storage.AvatarStorage
import com.mapprjct.exceptions.NetworkInterruptedException
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
import kotlin.coroutines.cancellation.CancellationException

class FileAvatarStorage(
    appConfig: AppConfig
) : AvatarStorage{

    val uploadDir = getOrCreateUploadDirectory(appConfig.avatarResourcePath)

    override suspend fun saveOrReplaceUserAvatar(
        user: User,
        fileExtension : String,
        avatarByteProvider: suspend () -> ByteReadChannel
    ): Result<String> {
        val tempFile = File(uploadDir, "${user.phone}.tmp.${UUID.randomUUID()}")
        val newFileName = "${user.phone}.$fileExtension"
        val oldAvatarPath = user.avatarFilename

        return runCatching {
            withContext(Dispatchers.IO) {
                avatarByteProvider().copyAndClose(tempFile.writeChannel())
            }
            removeOldAvatarIfExists(uploadDir,oldAvatarPath)
            val finalFile = File(uploadDir, newFileName)
            if(!tempFile.renameTo(finalFile)){
                throw IOException("Failed to rename temp → final: ${tempFile.path}")
            }
            newFileName
        }.onFailure { exception ->
            tempFile.delete()
            if (exception is CancellationException){
                throw exception
            }
        }.recover { exception->
            return when(exception){
                is IOException -> Result.failure(exception)
                else -> Result.failure(NetworkInterruptedException("Receive file failed because connection terminated"))
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