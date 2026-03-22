package com.mapprjct.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.mapprjct.com.mapprjct.utils.TransactionProvider
import com.mapprjct.model.dto.UserDTO
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.storage.AvatarStorage
import com.mapprjct.exceptions.domain.user.ValidateCredentialError
import com.mapprjct.exceptions.domain.user.DeleteUserAvatarError
import com.mapprjct.exceptions.domain.user.DeleteUserAvatarError.*
import com.mapprjct.exceptions.domain.user.FindUserAvatarError
import com.mapprjct.exceptions.domain.user.FindUserException
import com.mapprjct.exceptions.domain.user.UpdateAvatarError
import com.mapprjct.exceptions.domain.user.UpdateUserPasswordError
import com.mapprjct.exceptions.domain.user.CreateUserError
import com.mapprjct.exceptions.domain.user.UpdateUserError
import com.mapprjct.exceptions.storage.UpdateAvatarFileError
import com.mapprjct.model.dto.UserCredentialsDTO
import com.mapprjct.model.datatype.Password
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.Username
import io.ktor.utils.io.ByteReadChannel
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.postgresql.util.PSQLState
import java.io.File
import java.io.FileNotFoundException

class UserService(
    val userRepository: UserRepository,
    val avatarStorage: AvatarStorage,
    val transactionProvider : TransactionProvider
) {
    suspend fun createUser(userCredentialsDTO : UserCredentialsDTO, username : Username) : Either<CreateUserError,UserDTO> = either {
        val userDTO = UserDTO(phone = userCredentialsDTO.phone, username = username)
        catch({
            transactionProvider.runInTransaction {
                userRepository.insert(userDTO = userDTO, password = userCredentialsDTO.password)
            }
        }){ ex ->
            when (ex) {
                is ExposedSQLException if ex.sqlState == PSQLState.UNIQUE_VIOLATION.state ->
                    raise(CreateUserError.UserAlreadyExists(userCredentialsDTO.phone.value))
                is ExposedSQLException -> raise(CreateUserError.Database(ex))
                else -> raise(CreateUserError.Unexpected(ex))
            }
        }
    }
    suspend fun validateCredentials(userCredentialsDTO : UserCredentialsDTO): Either<ValidateCredentialError, Boolean> = either{
        catch({
            transactionProvider.runInTransaction {
                val storedCredentials = userRepository.getUserCredentials(userCredentialsDTO.phone)
                storedCredentials != null && storedCredentials.password == userCredentialsDTO.password
            }
        }) { ex->
            when {
                ex is ExposedSQLException -> raise(ValidateCredentialError.Database(ex))
                else -> raise(ValidateCredentialError.Unexpected(ex))
            }
        }
    }
    suspend fun getUser(userPhone : RussiaPhoneNumber) : Either<FindUserException, UserDTO> = either{
        catch({
            transactionProvider.runInTransaction {
                userRepository.findUser(userPhone) ?: raise(FindUserException.UserNotFound(userPhone.value))
            }
        }) { ex->
            when {
                ex is ExposedSQLException -> raise(FindUserException.Database(ex))
                else -> raise(FindUserException.Unexpected(ex))
            }
        }

    }
    suspend fun updateUser(userDTO : UserDTO) : Either<UpdateUserError, UserDTO> = either{
        catch({
            transactionProvider.runInTransaction {
                userRepository.updateUser(userDTO) ?: raise(UpdateUserError.UserNotFound(userDTO.phone.value))
            }
        }) { ex->
            when {
                ex is ExposedSQLException -> raise(UpdateUserError.Database(ex))
                else -> raise(UpdateUserError.Unexpected(ex))
            }
        }

    }
    suspend fun updateUserPassword(oldCredentials: UserCredentialsDTO, newUserPassword : Password) : Either<UpdateUserPasswordError, UserCredentialsDTO> = either{
        catch({
            transactionProvider.runInTransaction {
                val userCredentials = userRepository.getUserCredentials(oldCredentials.phone)
                    ?: raise(UpdateUserPasswordError.UserNotFound(oldCredentials.phone.value))
                if (userCredentials.password != oldCredentials.password) {
                    raise(UpdateUserPasswordError.IncorrectPassword())
                }
                userRepository.updateUserPassword(
                    userPhone = oldCredentials.phone,
                    password = newUserPassword
                )
                oldCredentials.copy(password = newUserPassword)
            }
        }){ ex->
            when {
                ex is ExposedSQLException -> raise(UpdateUserPasswordError.Database(ex))
                else -> raise(UpdateUserPasswordError.Unexpected(ex))
            }
        }
    }
    //TODO Rollback filechange if database update failed
    suspend fun updateUserAvatar(
        userDTO: UserDTO,
        fileName : String,
        fileDataChannelProvider : suspend ()-> ByteReadChannel,
    ) : Either<UpdateAvatarError, UserDTO> = either {
        val allowedExtensions = getAllowedAvatarFormats()
        val extension = "." + fileName.substringAfterLast('.').lowercase()
        ensure(extension in allowedExtensions) {
            UpdateAvatarError.InvalidAvatarFormat(allowedExtensions)
        }
        getUser(userDTO.phone).getOrElse { findUserException ->
            when(findUserException){
                is FindUserException.Database -> raise(UpdateAvatarError.Database(findUserException.exception))
                is FindUserException.Unexpected -> raise(UpdateAvatarError.Unexpected(findUserException.cause))
                is FindUserException.UserNotFound -> raise(UpdateAvatarError.UserNotFound(userDTO.phone.value))
            }
        }

        val avatarFileName = avatarStorage.saveOrReplaceUserAvatar(
            userDTO = userDTO,
            fileExtension = extension,
            avatarByteProvider = fileDataChannelProvider
        ).mapLeft { error->
            when (error) {
                UpdateAvatarFileError.ConnectionTerminated -> raise(UpdateAvatarError.ConnectionTerminated)
                UpdateAvatarFileError.FilesystemError -> raise(UpdateAvatarError.FilesystemUnavailable)
                is UpdateAvatarFileError.Unexpected -> raise(UpdateAvatarError.Unexpected(error.cause))
            }
        }.bind()

        updateUser(userDTO = userDTO.copy(avatarFilename = avatarFileName)).getOrElse { error ->
            when(error){
                is UpdateUserError.Database -> raise(UpdateAvatarError.Database(error.exception))
                is UpdateUserError.Unexpected -> raise(UpdateAvatarError.Unexpected(error.cause))
                is UpdateUserError.UserNotFound -> raise(UpdateAvatarError.Unexpected(Throwable("User lost during operations. Fatal error")))
            }
        }
    }
    suspend fun getUserAvatar(userPhone : RussiaPhoneNumber) : Either<FindUserAvatarError, File> = either {
        val user = getUser(userPhone).mapLeft { error->
            when (error) {
                is FindUserException.UserNotFound -> raise(FindUserAvatarError.UserNotFound(userPhone.value))
                is FindUserException.Database -> raise(FindUserAvatarError.Database(error.exception))
                is FindUserException.Unexpected -> raise(FindUserAvatarError.Unexpected(error.cause))
            }
        }.bind()
        user.avatarFilename ?: raise( FindUserAvatarError.UserAvatarNotFound)
        avatarStorage.getUserAvatar(user.avatarFilename!!).getOrElse{
            raise(FindUserAvatarError.UserAvatarNotFound)
        }
    }
    suspend fun deleteUserAvatar(userPhone : RussiaPhoneNumber) : Either<DeleteUserAvatarError, UserDTO> = either {
        val user = getUser(userPhone).mapLeft { error->
            when (error) {
                is FindUserException.UserNotFound -> raise(UserNotFound)
                is FindUserException.Database -> raise( Database(error.exception))
                is FindUserException.Unexpected -> raise( Unexpected(error.cause))
            }
        }.bind()
        val filename = ensureNotNull(user.avatarFilename) { UserAvatarNotFound }
        avatarStorage.deleteAvatar(filename).getOrElse { iOException ->
            when(iOException){
                is FileNotFoundException -> raise(UserAvatarNotFound)
                else -> raise(FileSystemUnavailable)
            }
        }
        transactionProvider.runInTransaction {
            updateUser(user.copy(avatarFilename = null)).mapLeft { error ->
                when (error) {
                    is UpdateUserError.Database -> raise(Database(error.exception))
                    is UpdateUserError.Unexpected -> raise(Unexpected(error.cause))
                    is UpdateUserError.UserNotFound -> raise(UserNotFound)
                }
            }.bind()
        }
    }
    fun getAllowedAvatarFormats() = listOf(".jpg", ".jpeg", ".png")
}