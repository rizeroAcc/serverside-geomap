package com.mapprjct.database.storage

import arrow.core.Either
import com.mapprjct.exceptions.storage.UpdateAvatarFileError
import com.mapprjct.model.dto.UserDTO
import io.ktor.utils.io.ByteReadChannel
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

interface AvatarStorage {

    suspend fun saveOrReplaceUserAvatar(
        userDTO : UserDTO,
        fileExtension : String,
        avatarByteProvider: suspend () -> ByteReadChannel
    ) : Either<UpdateAvatarFileError, String>
    /**
     * @throws java.io.FileNotFoundException - if avatar not found
     * */
    suspend fun getUserAvatar(avatarFilename : String) : Either<FileNotFoundException,File>
    /**
     * @throws java.io.FileNotFoundException - if avatar not found
     * @throws java.io.IOException - if filesystem error
     * */
    suspend fun deleteAvatar(avatarFilename : String) : Either<IOException,Unit>
    suspend fun getUploadDirectory() : File
}