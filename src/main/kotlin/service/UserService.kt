package com.mapprjct.service

import com.mapprjct.AppConfig
import com.mapprjct.model.dto.User
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.storage.AvatarStorage
import com.mapprjct.exceptions.NetworkInterruptedException
import com.mapprjct.exceptions.domain.user.CredentialsValidationException
import com.mapprjct.exceptions.domain.user.DeleteUserAvatarException
import com.mapprjct.exceptions.domain.user.DeleteUserAvatarException.*
import com.mapprjct.exceptions.domain.user.FindUserAvatarException
import com.mapprjct.exceptions.domain.user.FindUserException
import com.mapprjct.exceptions.domain.user.UpdateAvatarException
import com.mapprjct.exceptions.domain.user.UpdateUserPasswordException
import com.mapprjct.exceptions.domain.user.UserCreationException
import com.mapprjct.exceptions.domain.user.UserUpdateException
import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.model.datatype.Password
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.Username
import com.mapprjct.utils.Either
import com.mapprjct.utils.getOrElse
import com.mapprjct.utils.toEither
import io.ktor.utils.io.ByteReadChannel
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.postgresql.util.PSQLState
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class UserService(
    val userRepository: UserRepository,
    val avatarStorage: AvatarStorage,
    val database: Database,
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
                        UserCreationException.Database(exception)
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
                storedCredentials != null && storedCredentials.password == userCredentials.password
            }
        }.toEither { exception->
            when(exception){
                is ExposedSQLException -> CredentialsValidationException.Database(exception)
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
                is ExposedSQLException -> FindUserException.Database(exception)
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
    suspend fun updateUserPassword(oldCredentials: UserCredentials, newUserPassword : Password) : Either<UserCredentials, UpdateUserPasswordException>{
        return runCatching {
            suspendTransaction {
                val userCredentials = userRepository.getUserCredentials(oldCredentials.phone)
                    ?: throw UpdateUserPasswordException.UserNotFound(phone = oldCredentials.phone.value,)
                if (userCredentials.password != oldCredentials.password) {
                    throw UpdateUserPasswordException.IncorrectPassword()
                }
                userRepository.updateUserPassword(
                    userPhone = oldCredentials.phone,
                    password = newUserPassword
                )
                oldCredentials.copy(password = newUserPassword)
            }
        }.toEither { error->
            when(error){
                is ExposedSQLException -> UpdateUserPasswordException.DatabaseError(error)
                else -> UpdateUserPasswordException.Unexpected(error)
            }
        }
    }
    suspend fun updateUserAvatar(
        user: User,
        fileName : String,
        fileDataChannelProvider : suspend ()-> ByteReadChannel,
    ) : Either<User, UpdateAvatarException> = runCatching {
        val fileExtension = fileName.substringAfterLast('.').lowercase()
        if (!fileIsImage(fileName)) {
            throw UpdateAvatarException.InvalidAvatarFormat(listOf(".jpg",".jpeg",".png"))
        }

        val avatarFileName = avatarStorage.saveOrReplaceUserAvatar(
            user = user,
            fileExtension = fileExtension,
            avatarByteProvider = fileDataChannelProvider
        ).getOrThrow()

        val updatedUser = updateUser(user = user.copy(avatarFilename = avatarFileName)).getOrElse { error ->
            when(error){
                is UserUpdateException.DatabaseError -> throw UpdateAvatarException.DatabaseError(error.exception)
                is UserUpdateException.Unexpected -> throw UpdateAvatarException.Unexpected(error)
                is UserUpdateException.UserNotFound -> throw UpdateAvatarException.UserNotFound(error.phone)
            }
        }

        updatedUser
    }.toEither { error->
        when(error){
            is IOException -> UpdateAvatarException.FilesystemUnavailable()
            //TODO может переписать NetworkInterruptedException чтобы он хранил cause
            is NetworkInterruptedException -> UpdateAvatarException.ConnectionTerminated()
            is ExposedSQLException -> UpdateAvatarException.DatabaseError(error)
            else -> UpdateAvatarException.Unexpected(error)
        }
    }
    suspend fun getUserAvatar(userPhone : RussiaPhoneNumber) : Either<File, FindUserAvatarException> {
        return runCatching {
            val user = getUser(userPhone).getOrElse { error->
                when (error) {
                    is FindUserException.UserNotFound -> throw FindUserAvatarException.UserNotFound(userPhone.value)
                    else -> throw error
                }
            }
            user.avatarFilename ?: throw FindUserAvatarException.UserAvatarNotFound()
            avatarStorage.getUserAvatar(user.avatarFilename!!).getOrThrow()
        }.toEither { error->
            when (error){
                is FileNotFoundException -> FindUserAvatarException.UserAvatarNotFound()
                is ExposedSQLException -> FindUserAvatarException.DatabaseError(error)
                else -> FindUserAvatarException.Unexpected(error)
            }
        }
    }
    suspend fun deleteUserAvatar(userPhone : RussiaPhoneNumber) : Either<User, DeleteUserAvatarException>{
        return runCatching {
            val user = getUser(userPhone).getOrElse { error->
                when (error) {
                    is FindUserException.UserNotFound -> throw UserNotFound(userPhone.value)
                    is FindUserException.Database -> throw DatabaseError(error.exception)
                    is FindUserException.Unexpected -> throw Unexpected(error.cause)
                }
            }
            user.avatarFilename ?: throw UserAvatarNotFound()
            suspendTransaction(database) {
                updateUser(user.copy(avatarFilename = null)).getOrElse { error ->
                    when (error) {
                        is UserUpdateException.DatabaseError -> throw DatabaseError(error.exception)
                        is UserUpdateException.Unexpected -> throw Unexpected(error.cause)
                        is UserUpdateException.UserNotFound -> throw UserNotFound(userPhone.value)
                    }
                }
                avatarStorage.deleteAvatar(user.avatarFilename!!).getOrThrow()
                user.copy(avatarFilename = null)
            }
        }.toEither { error->
            when (error){
                is FileNotFoundException -> UserAvatarNotFound()
                is IOException -> FileSystemUnavailable()
                is ExposedSQLException -> DatabaseError(error)
                else -> Unexpected(error)
            }
        }
    }
    private fun fileIsImage(filename : String) : Boolean {
        return filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png")
    }
}