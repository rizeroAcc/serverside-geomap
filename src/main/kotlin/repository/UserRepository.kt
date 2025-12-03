package com.mapprjct.repository

import com.mapprjct.database.users.UserDTO
import com.mapprjct.database.dao.UserDAO

class UserRepository(val userDAO: UserDAO) {
    suspend fun registerNewUser(user : UserDTO) : Result<UserDTO>{
        val existingUser = userDAO.getUserByPhone(user.phone)
        if (existingUser == null){
            userDAO.insert(user)
            return Result.success(user)
        }else{
            return Result.failure(Exception("User already exists"))
        }
    }
    suspend fun isAuthorizationValid(user : UserDTO) : Boolean{
        val existingUser = userDAO.getUserByPhone(user.phone)
        if (existingUser == null){
            return false
        }else{
            return existingUser.passwordHash == user.passwordHash
        }
    }
}