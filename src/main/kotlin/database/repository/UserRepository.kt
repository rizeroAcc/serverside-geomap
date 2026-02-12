package com.mapprjct.database.repository

import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.model.dto.User
import com.mapprjct.model.value.Password
import com.mapprjct.model.value.RussiaPhoneNumber

interface UserRepository {
    suspend fun insert(user: User, password : Password)
    suspend fun getUser(phone : RussiaPhoneNumber) : User?
    suspend fun getUserCredentials(phone : RussiaPhoneNumber) : UserCredentials?
    suspend fun updateUserPassword(userPhone : RussiaPhoneNumber, password : Password) : Int
    suspend fun updateUser(user : User) : User?
}