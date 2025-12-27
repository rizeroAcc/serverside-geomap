package com.mapprjct.service

import com.mapprjct.exceptions.ElementAlreadyExistsException
import com.mapprjct.model.dto.User
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.exceptions.domain.InvalidDataException
import com.mapprjct.utils.CredentialsValidator
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

class UserService(
    val userRepository: UserRepository,
    val database: Database,
) {

    suspend fun createUser(userCredentials : UserCredentials, username : String) : Result<UserCredentials> {

        if (!CredentialsValidator.validatePhone(userCredentials.phone)){
            return Result.failure(InvalidDataException(details =
                """
                Incorrect user phone format. 
                It must start with '+7' or '8' and have valid length
                """.trimIndent()))
        }
        if (!CredentialsValidator.validatePassword(userCredentials.password)){
            return Result.failure(InvalidDataException(details = "Password must have at least eight characters"))
        }
        suspendTransaction {

        }
        val existingUser = userRepository.getUser(userCredentials.phone)

        if (existingUser == null){
            val user = User(phone = userCredentials.phone, username = username)
            userRepository.insert(user = user, password = userCredentials.password)
            return Result.success(userCredentials)
        }else{
            return Result.failure(ElementAlreadyExistsException("User already exists"))
        }
    }

    suspend fun validateCredentials(userCredentials : UserCredentials): Boolean {
        val existingUser = userRepository.getUserCredentials(userCredentials.phone)
        return existingUser != null && existingUser.password == userCredentials.password
    }
    suspend fun getUser(userPhone : String) : User?{
        val existingUser = userRepository.getUser(userPhone)
        return existingUser
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