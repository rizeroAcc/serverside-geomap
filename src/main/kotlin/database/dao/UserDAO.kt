package com.mapprjct.database.dao

import com.mapprjct.dto.UserCredentials
import com.mapprjct.dto.User

interface UserDAO {
    suspend fun insert(userCredentials : UserCredentials, username : String) : Int
    suspend fun getUser(phone : String) : User?
    suspend fun getUserCredentials(phone : String) : UserCredentials?
    suspend fun updateUser(user : User) : User?
}