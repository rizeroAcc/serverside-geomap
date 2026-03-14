package com.mapprjct.database.storage

import arrow.core.Either
import com.mapprjct.exceptions.storage.UpdateAvatarFileError
import com.mapprjct.model.dto.User
import io.ktor.utils.io.ByteReadChannel
import java.io.File

interface AvatarStorage {

    suspend fun saveOrReplaceUserAvatar(user : User, fileExtension : String, avatarByteProvider: suspend () -> ByteReadChannel) : Either<UpdateAvatarFileError, String>
    /**
     * @throws java.io.FileNotFoundException - if avatar not found
     * */
    suspend fun getUserAvatar(avatarFilename : String) : Result<File>
    /**
     * @throws java.io.FileNotFoundException - if avatar not found
     * @throws java.io.IOException - if filesystem error
     * */
    suspend fun deleteAvatar(avatarFilename : String) : Result<Unit>
    suspend fun getUploadDirectory() : File
}