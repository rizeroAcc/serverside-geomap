package com.mapprjct.service

import com.mapprjct.model.dto.User
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.exceptions.user.UserDMLExceptions
import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.exceptions.user.UserValidationException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
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
                    throw IllegalArgumentException("Incorrect password")
                }
                userRepository.updateUserPassword(
                    userPhone = oldCredentials.phone,
                    password = newUserPassword
                )
                oldCredentials.copy(password = newUserPassword)
            }
        }
    }

}