package com.mapprjct.database.repository

import com.mapprjct.database.entity.UserEntity
import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.model.dto.User

interface UserRepository {
    suspend fun insert(user: User, password : String)
    suspend fun getUser(phone : String) : User?
    suspend fun getUserCredentials(phone : String) : UserCredentials?
    suspend fun updateUserPassword(userPhone : String, password : String) : UserCredentials?
    suspend fun updateUser(user : User) : User?
}