package com.mapprjct.service

import com.mapprjct.AppConfig
import com.mapprjct.model.dto.User
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.storage.AvatarStorage
import com.mapprjct.exceptions.UserCreationException
import com.mapprjct.exceptions.user.UserDMLExceptions
import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.exceptions.user.UserValidationException
import com.mapprjct.model.value.Password
import com.mapprjct.model.value.RussiaPhoneNumber
import com.mapprjct.model.value.Username
import com.mapprjct.utils.DatabaseDataResult
import com.mapprjct.utils.DatabaseDataResult.Companion.databaseError
import com.mapprjct.utils.DatabaseDataResult.Companion.domainError
import com.mapprjct.utils.accessDatabaseData
import io.ktor.utils.io.ByteReadChannel
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.postgresql.util.PSQLState
import java.io.File
import java.io.IOException

class UserService(
    val userRepository: UserRepository,
    val avatarStorage: AvatarStorage,
    val database: Database,
    val appConfig: AppConfig,
) {

    suspend fun createUser(userCredentials : UserCredentials, username : Username) : DatabaseDataResult<User, UserCreationException> {

        return accessDatabaseData(
            database,
            databaseExceptionMapper = { exposedException->

            }){
            val existingUser = userRepository.getUser(userCredentials.phone)
            if (existingUser!=null){
                throw UserDMLExceptions.UserAlreadyExistsException(userCredentials.phone.value)
            }
            val user = User(phone = userCredentials.phone, username = username)
            userRepository.insert(user = user, password = userCredentials.password)
            user
        }

        return runCatching {
            DatabaseDataResult.success(
                suspendTransaction(database) {

                }
            )
        }.getOrElse { exception ->
            val sqlException = exception as ExposedSQLException
            if (sqlException.sqlState == PSQLState.UNIQUE_VIOLATION.state) {
                domainError<UserCreationException>(UserCreationException.UserAlreadyExists())
            }else{
                databaseError(sqlException)
            }
        }
    }
    /**
     * @return [org.jetbrains.exposed.v1.exceptions.ExposedSQLException] - if database unavailable
     * */
    suspend fun validateCredentials(userCredentials : UserCredentials): DatabaseDataResult<Boolean, Nothing> {
        return runCatching {
            DatabaseDataResult.success(
                suspendTransaction {
                    val existingUser = userRepository.getUserCredentials(userCredentials.phone)
                    val isUserExistingAndPasswordCorrect = existingUser != null && existingUser.password == userCredentials.password
                    isUserExistingAndPasswordCorrect
                }
            )
        }
    }
    /**
     * @return [org.jetbrains.exposed.v1.exceptions.ExposedSQLException] - if database unavailable
     * @throws UserDMLExceptions.UserNotFoundException - if user not found
     * */
    suspend fun getUser(userPhone : RussiaPhoneNumber) : Result<User>{
        return runCatching {
            suspendTransaction {
                userRepository.getUser(userPhone) ?: throw UserDMLExceptions.UserNotFoundException(userPhone.value)
            }
        }
    }
    /**
     * Update user info **without phone**
     * @return User - if update success
     * @throws UserValidationException - if new user info invalid
     * @throws UserDMLExceptions.UserNotFoundException - if user not found
     * @throws org.jetbrains.exposed.v1.exceptions.ExposedSQLException - if database unavailable
     * */
    suspend fun updateUser(user : User) : Result<User?>{
        return runCatching {
            suspendTransaction {
                userRepository.updateUser(user) ?: throw UserDMLExceptions.UserNotFoundException(user.phone.value)
            }
        }
    }
    /**
     * @return [model.dto.UserCredentials] - if update success
     * @throws UserDMLExceptions.UserNotFoundException - if user not found
     * @throws IllegalArgumentException - if old password wrong
     * @throws org.jetbrains.exposed.v1.exceptions.ExposedSQLException - if database unavailable
     * */
    suspend fun updateUserPassword(oldCredentials: UserCredentials, newUserPassword : Password) : Result<UserCredentials>{
        return runCatching {
            suspendTransaction {
                val userCredentials = userRepository.getUserCredentials(oldCredentials.phone) ?: throw
                    UserDMLExceptions.UserNotFoundException(
                        phone = oldCredentials.phone.value,
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
        val fileExtension = fileName.substringAfterLast('.').lowercase()
        if (!fileIsImage(fileName)) {
            throw IllegalArgumentException("Invalid file format. Allowed formats: jpg, jpeg, png")
        }

        val avatarFileName = avatarStorage.saveOrReplaceUserAvatar(
            user = user,
            fileExtension = fileExtension,
            avatarByteProvider = fileDataChannelProvider
        ).getOrThrow()

        val updatedUser = updateUser(user = user.copy(avatarFilename = avatarFileName)).getOrThrow()

        updatedUser!!
    }

    /**
     * @throws UserDMLExceptions.UserAvatarNotFoundException - if user haven't avatar
     * @throws java.io.FileNotFoundException - if file must exist but not found
     * */
    suspend fun getUserAvatar(userPhone : RussiaPhoneNumber) : Result<File> {
        return runCatching {
            val user = getUser(userPhone).getOrElse {
                throw UserDMLExceptions.UserNotFoundException(phone = userPhone.value)
            }
            user.avatarFilename ?: throw UserDMLExceptions.UserAvatarNotFoundException()
            avatarStorage.getUserAvatar(user.avatarFilename!!).getOrThrow()
        }
    }
    /**
     * @throws UserDMLExceptions.UserAvatarNotFoundException - if user haven't avatar
     * @throws java.io.FileNotFoundException - if file must exist but not found
     * @throws org.jetbrains.exposed.v1.exceptions.ExposedSQLException
     * */
    suspend fun deleteUserAvatar(user: User) : Result<Unit>{
        return runCatching {
            user.avatarFilename ?: throw UserDMLExceptions.UserAvatarNotFoundException()
            suspendTransaction(database) {
                updateUser(user.copy(avatarFilename = null))
                avatarStorage.deleteAvatar(user.avatarFilename!!).getOrThrow()
            }
        }
    }

    private fun fileIsImage(filename : String) : Boolean {
        return filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png")
    }
}