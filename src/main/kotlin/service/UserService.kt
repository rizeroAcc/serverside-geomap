package com.mapprjct.service

import com.mapprjct.AppConfig
import com.mapprjct.model.dto.User
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.storage.AvatarStorage
import com.mapprjct.exceptions.user.UserDMLExceptions
import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.exceptions.user.UserValidationException
import io.ktor.utils.io.ByteReadChannel
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.io.File
import java.io.IOException
import kotlin.Result.Companion.failure

class UserService(
    val userRepository: UserRepository,
    val avatarStorage: AvatarStorage,
    val database: Database,
    val appConfig: AppConfig,
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
     * @throws UserDMLExceptions.UserNotFoundException - if user not found
     * */
    suspend fun getUser(userPhone : String) : Result<User>{
        return runCatching {
            suspendTransaction {
                userRepository.getUser(userPhone) ?: throw UserDMLExceptions.UserNotFoundException(userPhone)
            }
        }
    }
    /**
     * Update user info **without phone**
     *
     * @return User - if update success
     *
     * @throws UserDMLExceptions.UserNotFoundException - if user not found
     *
     * @throws org.jetbrains.exposed.v1.exceptions.ExposedSQLException - if database unavailable
     * */
    suspend fun updateUser(user : User) : Result<User?>{
        return runCatching {
            suspendTransaction {
                userRepository.updateUser(user) ?: throw UserDMLExceptions.UserNotFoundException(user.phone)
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
    /**
     * @throws IllegalArgumentException - if file format invalid
     * @throws IOException - if file access error
     * @throws com.mapprjct.exceptions.NetworkInterruptedException - if channel broken
     * @throws org.jetbrains.exposed.v1.exceptions.ExposedSQLException - if database unavailable
     * */
    suspend fun updateUserAvatar(
        user: User,
        fileName : String,
        fileDataChannelProvider : suspend ()-> ByteReadChannel,
    ) : Result<User> = runCatching {
        var avatarFileName: String? = null
        var updatedUser : User? = null

        val fileExtension = fileName.substringAfterLast('.').lowercase()
        if (!fileIsImage(fileName)) {
            throw IllegalArgumentException("Invalid file format. Allowed formats: jpg, jpeg, png")
        }

        val foo =  avatarStorage.saveOrReplaceUserAvatar(
            user = user,
            fileExtension = fileExtension,
            avatarByteProvider = fileDataChannelProvider
        )

        avatarFileName = foo.getOrThrow()

        updatedUser = updateUser(user = user.copy(avatarFilename = avatarFileName)).getOrThrow()

        updatedUser!!
    }

    /**
     * @throws UserDMLExceptions.UserAvatarNotFoundException - if user haven't avatar
     * @throws java.io.FileNotFoundException - if file must exist but not found
     * */
    suspend fun getUserAvatar(userPhone : String) : Result<File> {
        return runCatching {
            val user = getUser(userPhone).getOrThrow()
            user.avatarFilename ?: throw UserDMLExceptions.UserAvatarNotFoundException()
            avatarStorage.getUserAvatar(user.avatarFilename).getOrThrow()
        }
    }

    private fun fileIsImage(filename : String) : Boolean {
        return filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png")
    }

}