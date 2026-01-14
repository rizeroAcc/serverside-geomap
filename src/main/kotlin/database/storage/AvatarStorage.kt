package com.mapprjct.database.storage

import com.mapprjct.model.dto.User
import io.ktor.utils.io.ByteReadChannel
import java.io.File

interface AvatarStorage {
    /**
     * @throws java.io.IOException - if filesystem error
     * @throws com.mapprjct.exceptions.NetworkInterruptedException - if connection terminated
     * @return new avatar path
     * */
    suspend fun saveOrReplaceUserAvatar(user : User, fileExtension : String, avatarByteProvider: suspend () -> ByteReadChannel) : Result<String>
    /**
     * @throws java.io.FileNotFoundException - if avatar not found
     * */
    suspend fun getUserAvatar(avatarFilename : String) : Result<File>
    suspend fun getUploadDirectory() : File
}