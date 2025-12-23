package com.mapprjct.database.dao

import com.mapprjct.dto.Avatar
import com.mapprjct.dto.UserCredentials
import com.mapprjct.dto.User

interface UserDAO {
    suspend fun insert(user: User, password : String)
    suspend fun getUser(phone : String) : User?
    suspend fun getUserCredentials(phone : String) : UserCredentials?
    suspend fun updateUserCredentials(userPhone : String, newCredentials : UserCredentials) : Int
    suspend fun updateUser(user : User) : Int
}