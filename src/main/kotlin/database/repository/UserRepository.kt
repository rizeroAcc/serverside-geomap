package com.mapprjct.database.repository

import com.mapprjct.dto.UserCredentials
import com.mapprjct.dto.User

interface UserRepository {
    suspend fun insert(user: User, password : String)
    suspend fun getUser(phone : String) : User?
    suspend fun getUserCredentials(phone : String) : UserCredentials?
    suspend fun updateUserPassword(userPhone : String, password : String) : Int
    suspend fun updateUser(user : User) : Int
}