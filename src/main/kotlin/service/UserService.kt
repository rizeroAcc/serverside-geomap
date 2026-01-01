package com.mapprjct.service

import com.mapprjct.model.dto.User
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.exceptions.ElementAlreadyExistsException
import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.exceptions.UserValidationException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import kotlin.Result.Companion.failure

class UserService(
    val userRepository: UserRepository,
    val database: Database,
) {

    /**
     * @return [UserValidationException] - if user data incorrect
     * @return [ElementAlreadyExistsException] - if user with phone already exists
     * */
    suspend fun createUser(userCredentials : UserCredentials, username : String) : Result<User> {
        userCredentials.validate().onFailure { return failure(it) }
        if (username.isBlank()){
            return failure(UserValidationException.InvalidUsername())
        }

        return suspendTransaction(database) {
            val existingUser = userRepository.getUser(userCredentials.phone)
            if (existingUser!=null){
                return@suspendTransaction failure(ElementAlreadyExistsException())
            }
            val user = User(phone = userCredentials.phone, username = username)
            userRepository.insert(user = user, password = userCredentials.password)
            return@suspendTransaction Result.success(user)
        }

    }

    suspend fun validateCredentials(userCredentials : UserCredentials): Boolean {
        return suspendTransaction {
            val existingUser = userRepository.getUserCredentials(userCredentials.phone)
            val isUserExistingAndPasswordCorrect = existingUser != null && existingUser.password == userCredentials.password
            return@suspendTransaction isUserExistingAndPasswordCorrect
        }
    }
    suspend fun getUser(userPhone : String) : User?{
        return suspendTransaction {
            return@suspendTransaction userRepository.getUser(userPhone)
        }
    }
    suspend fun updateUser(user : User) : Result<User>{
        return runCatching {
            userRepository.updateUser(user)
            user
        }
    }
    suspend fun updateUserPassword(oldCredentials: UserCredentials, newUserPassword : String) : Result<UserCredentials>{
        val oldUserCredentials = userRepository.getUserCredentials(oldCredentials.phone) ?: return Result.failure(
            IllegalStateException("User does not exists")
        )
        if (oldUserCredentials.password != oldCredentials.password) {
            return Result.failure(IllegalArgumentException("Incorrect password"))
        }
        userRepository.updateUserPassword(
            userPhone = oldCredentials.phone,
            password = newUserPassword
        )
        return Result.success(oldCredentials.copy(password = newUserPassword))
    }

}