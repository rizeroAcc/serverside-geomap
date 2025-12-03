package com.mapprjct.database.dao

import com.mapprjct.database.users.UserDTO

interface UserDAO {
    suspend fun insert(user : UserDTO) : Int
    suspend fun getUserByPhone(phone : String) : UserDTO?
}