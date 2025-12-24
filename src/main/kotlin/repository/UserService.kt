package com.mapprjct.repository

import com.mapprjct.ElementAlreadyExistsException
import com.mapprjct.dto.User
import com.mapprjct.database.dao.UserRepository
import com.mapprjct.dto.UserCredentials

class UserService(val userRepository: UserRepository) {



    suspend fun createUser(userCredentials : UserCredentials, username : String) : Result<UserCredentials>{
        //todo validate phone
        //todo hash password
        val existingUser = userRepository.getUser(userCredentials.phone)
        if (existingUser == null){
            val user = User(phone = userCredentials.phone, username = username)
            userRepository.insert(user = user, password = userCredentials.password)
            return Result.success(userCredentials)
        }else{
            return Result.failure(ElementAlreadyExistsException("User already exists"))
        }
    }
    suspend fun getUserCredentials(userCredentials : UserCredentials) : UserCredentials?{
        val existingUser = userRepository.getUserCredentials(userCredentials.phone)
        return existingUser?.let {
            if (it.password == userCredentials.password){
                it
            }else{
                null
            }
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