package com.mapprjct.repository

import com.mapprjct.ElementAlreadyExistsException
import com.mapprjct.dto.User
import com.mapprjct.database.dao.UserDAO
import com.mapprjct.dto.UserCredentials

class UserRepository(val userDAO: UserDAO) {

    suspend fun createUser(user : UserCredentials) : Result<UserCredentials>{
        //todo validate phone
        //todo hash password
        val existingUser = userDAO.getUser(user.phone)
        if (existingUser == null){
            userDAO.insert(user)
            return Result.success(user)
        }else{
            return Result.failure(ElementAlreadyExistsException("User already exists"))
        }
    }

    suspend fun getUserCredentials(userCredentials : UserCredentials) : UserCredentials?{
        val existingUser = userDAO.getUserCredentials(userCredentials.phone)
        return existingUser?.let {
            if (it.passwordHash == userCredentials.passwordHash){
                it
            }else{
                null
            }
        }
    }
    suspend fun getUser(userPhone : String) : User?{
        val existingUser = userDAO.getUser(userPhone)
        return existingUser
    }
    suspend fun updateUser(user : User) : Result<User>{
        val newUserData = userDAO.updateUser(user)
        return if(newUserData != null){
            Result.success(newUserData)
        }else{
            Result.failure(Exception("User does not exists"))
        }
    }
}