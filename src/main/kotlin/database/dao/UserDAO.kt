package com.mapprjct.database.dao

import com.mapprjct.dto.Avatar
import com.mapprjct.dto.UserCredentials
import com.mapprjct.dto.User

interface UserDAO {
    suspend fun insert(userCredentials : UserCredentials, username : String) : Int
    suspend fun getUser(phone : String) : User?
    suspend fun getUserCredentials(phone : String) : UserCredentials?
    suspend fun updateUserAvatar(user: User, avatar: Avatar) : User?
    suspend fun updateUserCredentials(userPhone : String, newCredentials : UserCredentials) : UserCredentials?
}