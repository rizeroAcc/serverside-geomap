package com.mapprjct.database.repository

import com.mapprjct.model.dto.UserCredentialsDTO
import com.mapprjct.model.dto.UserDTO
import com.mapprjct.model.datatype.Password
import com.mapprjct.model.datatype.RussiaPhoneNumber

interface UserRepository {
    suspend fun insert(userDTO: UserDTO, password : Password) : UserDTO
    suspend fun findUser(phone : RussiaPhoneNumber) : UserDTO?
    suspend fun getUserCredentials(phone : RussiaPhoneNumber) : UserCredentialsDTO?
    suspend fun updateUserPassword(userPhone : RussiaPhoneNumber, password : Password) : Password?
    suspend fun updateUser(userDTO : UserDTO) : UserDTO?
}