package com.mapprjct.repository

import com.mapprjct.ElementAlreadyExistsException
import com.mapprjct.dto.User
import com.mapprjct.database.dao.UserDAO
import com.mapprjct.dto.Avatar
import com.mapprjct.dto.UserCredentials

class UserRepository(val userDAO: UserDAO) {



    suspend fun createUser(userCredentials : UserCredentials, username : String) : Result<UserCredentials>{
        //todo validate phone
        //todo hash password
        val existingUser = userDAO.getUser(userCredentials.phone)
        if (existingUser == null){
            userDAO.insert(userCredentials = userCredentials, username = username)
            return Result.success(userCredentials)
        }else{
            return Result.failure(ElementAlreadyExistsException("User already exists"))
        }
    }

    suspend fun getUserCredentials(userCredentials : UserCredentials) : UserCredentials?{
        val existingUser = userDAO.getUserCredentials(userCredentials.phone)
        return existingUser?.let {
            if (it.password == userCredentials.password){
                it
            }else{
                null
            }
        }
    }
    suspend fun validateCredentials(userCredentials : UserCredentials): Boolean {
        val existingUser = userDAO.getUserCredentials(userCredentials.phone)
        return existingUser != null && existingUser.password == userCredentials.password
    }
    suspend fun getUser(userPhone : String) : User?{
        val existingUser = userDAO.getUser(userPhone)
        return existingUser
    }
    suspend fun updateUser(user : User, avatar: Avatar) : Result<User>{
        val newUserData = userDAO.updateUserAvatar(user, avatar)
        return if(newUserData != null){
            Result.success(newUserData)
        }else{
            Result.failure(Exception("User does not exists"))
        }
    }
    suspend fun updateUserPassword(oldCredentials: UserCredentials, newUserPassword : String) : Result<UserCredentials>{
        val oldUserCredentials = userDAO.getUserCredentials(oldCredentials.phone) ?: return Result.failure(
            IllegalStateException("User does not exists")
        )
        if (oldUserCredentials.password != oldCredentials.password) {
            return Result.failure(ElementAlreadyExistsException("Incorrect password"))
        }
        userDAO.updateUserCredentials(
            userPhone = oldCredentials.phone,
            newCredentials = oldCredentials.copy(password = newUserPassword)
        )
        return Result.success(oldCredentials.copy(password = newUserPassword))
    }
}