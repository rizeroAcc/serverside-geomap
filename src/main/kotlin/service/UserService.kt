package com.mapprjct.service

import com.mapprjct.model.dto.User
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.exceptions.user.UserDMLExceptions
import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.exceptions.user.UserValidationException
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.io.File
import kotlin.Result.Companion.failure

class UserService(
    val userRepository: UserRepository,
    val database: Database,
) {

    /**
     * @return [UserValidationException] - if user data incorrect
     *
     * [UserDMLExceptions.UserAlreadyExistsException] - if user with phone already exists
     *
     * [org.jetbrains.exposed.v1.exceptions.ExposedSQLException] - if database unavailable
     * */
    suspend fun createUser(userCredentials : UserCredentials, username : String) : Result<User> {
        userCredentials.validate().onFailure { return failure(it) }
        if (username.isBlank()){
            return failure(UserValidationException.InvalidUsername())
        }

        return runCatching {
            suspendTransaction(database) {
                val existingUser = userRepository.getUser(userCredentials.phone)
                if (existingUser!=null){
                    throw UserDMLExceptions.UserAlreadyExistsException(userCredentials.phone)
                }
                val user = User(phone = userCredentials.phone, username = username)
                userRepository.insert(user = user, password = userCredentials.password)
                return@suspendTransaction user
            }
        }

    }
    /**
     * @return [org.jetbrains.exposed.v1.exceptions.ExposedSQLException] - if database unavailable
     * */
    suspend fun validateCredentials(userCredentials : UserCredentials): Result<Boolean> {
        return runCatching {
            suspendTransaction {
                val existingUser = userRepository.getUserCredentials(userCredentials.phone)
                val isUserExistingAndPasswordCorrect = existingUser != null && existingUser.password == userCredentials.password
                isUserExistingAndPasswordCorrect
            }
        }
    }
    /**
     * @return [org.jetbrains.exposed.v1.exceptions.ExposedSQLException] - if database unavailable
     * */
    suspend fun getUser(userPhone : String) : Result<User?>{
        return runCatching {
            suspendTransaction {
                userRepository.getUser(userPhone)
            }
        }
    }
    /**
     * Update user info **without phone**
     *
     * @return User - if update success
     *
     *  null - if updating error occured
     *
     *  [org.jetbrains.exposed.v1.exceptions.ExposedSQLException] - if database unavailable
     * */
    suspend fun updateUser(user : User) : Result<User?>{
        return runCatching {
            suspendTransaction {
                userRepository.updateUser(user)
            }
        }
    }
    /**
     * @return [UserCredentials] - if update success
     *
     * [UserDMLExceptions.UserNotFoundException] - if user not found
     *
     * [IllegalArgumentException] - if old password wrong
     *
     *  [org.jetbrains.exposed.v1.exceptions.ExposedSQLException] - if database unavailable
     * */
    suspend fun updateUserPassword(oldCredentials: UserCredentials, newUserPassword : String) : Result<UserCredentials>{
        return runCatching {
            suspendTransaction {
                val userCredentials = userRepository.getUserCredentials(oldCredentials.phone) ?: throw
                    UserDMLExceptions.UserNotFoundException(
                        phone = oldCredentials.phone,
                    )
                if (userCredentials.password != oldCredentials.password) {
                    throw IllegalArgumentException("Incorrect old password")
                }
                userRepository.updateUserPassword(
                    userPhone = oldCredentials.phone,
                    password = newUserPassword
                )
                oldCredentials.copy(password = newUserPassword)
            }
        }
    }
    /***/
    suspend fun updateUserAvatar(user: User, multipart: MultiPartData) : Result<User>{
        var avatarFileName: String? = null

        multipart.forEachPart { part ->
            when (part) {
                is PartData.FileItem -> {
                    val originalFileName = part.originalFileName as String
                    val fileExtension = originalFileName.substringAfterLast('.').lowercase()
                    if (!fileIsImage(originalFileName)) {
                        throw IllegalArgumentException("Allowed formats: jpg, png")
                    }

                    // Генерация имени файла
                    val fileName = "${user.phone}.$fileExtension"
                    val uploadDir = getOrCreateUploadDirectory("api/uploads/avatars")

                    val oldAvatarPath = user.avatarFilename
                    removeOldFile(uploadDir,oldAvatarPath)

                    // Create new file
                    val targetFile = File(uploadDir, fileName)
                    //Write image
                    part.provider().copyAndClose((targetFile.writeChannel()))

                    // Обновляем в базе данных
                    avatarFileName = fileName
                    updateUser(user = user.copy(avatarFilename = avatarFileName))
                }
                else -> {}
            }
            part.dispose()
        }
        avatarFileName ?: return failure<User>(IllegalArgumentException("avatar file name is null"))

        return Result.success(user.copy(avatarFilename = "api/uploads/avatars/$avatarFileName"))
    }

    private fun fileIsImage(filename : String) : Boolean {
        return filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png")
    }
    private fun getOrCreateUploadDirectory(relatePath : String) : File {
        val directory = File(relatePath)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    private fun removeOldFile(directory : File, oldFilename : String?) {
        oldFilename?.let { oldFileName ->
            val oldFile = File(directory, oldFileName)
            if (oldFile.exists()) {
                oldFile.delete()
            }
        }
    }
}