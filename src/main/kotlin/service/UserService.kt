package com.mapprjct.service

import com.mapprjct.AppConfig
import com.mapprjct.model.dto.User
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.storage.AvatarStorage
import com.mapprjct.exceptions.user.CredentialsValidationException
import com.mapprjct.exceptions.user.FindUserException
import com.mapprjct.exceptions.user.UserCreationException
import com.mapprjct.exceptions.user.UserUpdateException
import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.model.value.Password
import com.mapprjct.model.value.RussiaPhoneNumber
import com.mapprjct.model.value.Username
import com.mapprjct.utils.Either
import com.mapprjct.utils.getOrElse
import com.mapprjct.utils.toEither
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

    suspend fun createUser(userCredentials : UserCredentials, username : Username) : Either<User, UserCreationException> {
        return runCatching {
                suspendTransaction(database) {
                    val user = User(phone = userCredentials.phone, username = username)
                    userRepository.insert(user = user, password = userCredentials.password)
                    user
                }
        }.toEither { exception->
            when(exception){
                is ExposedSQLException -> {
                    if (exception.sqlState == PSQLState.UNIQUE_VIOLATION.state){
                        UserCreationException.UserAlreadyExists(userCredentials.phone.value)
                    }else{
                        UserCreationException.DatabaseError(exception)
                    }
                }
                else -> UserCreationException.Unexpected(exception)
            }
        }
    }

    suspend fun validateCredentials(userCredentials : UserCredentials): Either<Boolean, CredentialsValidationException> {
        return runCatching {
            suspendTransaction {
                val storedCredentials = userRepository.getUserCredentials(userCredentials.phone)
                val isUserExistingAndPasswordCorrect = storedCredentials != null && storedCredentials.password == userCredentials.password
                isUserExistingAndPasswordCorrect
            }
        }.toEither { exception->
            when(exception){
                is ExposedSQLException -> CredentialsValidationException.DatabaseError(exception)
                else -> CredentialsValidationException.Unexpected(exception)
            }
        }
    }

    suspend fun getUser(userPhone : RussiaPhoneNumber) : Either<User, FindUserException>{
        return runCatching {
            suspendTransaction {
                userRepository.getUser(userPhone) ?: throw FindUserException.UserNotFound(userPhone.value)
            }
        }.toEither { exception->
            when(exception){
                is ExposedSQLException -> FindUserException.DatabaseError(exception)
                else -> FindUserException.Unexpected(exception)
            }
        }
    }

    suspend fun updateUser(user : User) : Either<User, UserUpdateException>{
        return runCatching {
            suspendTransaction {
                userRepository.updateUser(user) ?: throw UserUpdateException.UserNotFound(user.phone.value)
            }
        }.toEither { error->
            when(error){
                is ExposedSQLException -> UserUpdateException.DatabaseError(error)
                else -> UserUpdateException.Unexpected(error)
            }
        }
    }
    /**
     * @return [model.dto.UserCredentials] - if update success
     * @throws UserDMLExceptions.UserNotFoundException - if user not found
     * @throws IllegalArgumentException - if old password wrong
     * @throws org.jetbrains.exposed.v1.exceptions.ExposedSQLException - if database unavailable
     * */
    suspend fun updateUserPassword(oldCredentials: UserCredentials, newUserPassword : Password) : Either<UserCredentials, UserUpdateException>{
        return runCatching {
            suspendTransaction {
                val userCredentials = userRepository.getUserCredentials(oldCredentials.phone) ?: throw
                UserUpdateException.UserNotFound(
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
        }.toEither { error->
            when(error){
                is ExposedSQLException -> UserUpdateException.DatabaseError(error)
                else -> UserUpdateException.Unexpected(error)
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
            val user = getUser(userPhone).getOrElse { error->
                when (error) {
                    is FindUserException.UserNotFound -> throw UserDMLExceptions.UserNotFoundException(phone = userPhone.value)
                    else -> throw error
                }
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