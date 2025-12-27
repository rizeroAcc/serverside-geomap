package com.mapprjct.database.daoimpl

import com.mapprjct.database.entity.UserEntity
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.tables.UserTable
import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.model.dto.User
import com.mapprjct.utils.replaceRussiaCountryCode
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

class UserRepositoryImpl(val database: Database) : UserRepository {

    override suspend fun insert(user: User, password : String){
        UserEntity.new(
            id = user.phone.replaceRussiaCountryCode()
        ) {
            this.username = user.username
            this.password = password
            this.avatar = user.avatarPath
        }
    }

    override suspend fun getUser(phone : String) : User?{
        return UserEntity.findById(
            phone.replaceRussiaCountryCode()
        )?.toUser()
    }

    override suspend fun getUserCredentials(phone: String): UserCredentials? {
        return UserEntity.findById(
            phone.replaceRussiaCountryCode()
        )?.toUserCredentials()
    }


    override suspend fun updateUserPassword(userPhone : String, password: String): UserCredentials? {
        return UserEntity.findSingleByAndUpdate(
            UserTable.phone eq userPhone.replaceRussiaCountryCode()
        ){ userEntity ->
            userEntity.password = password
        }?.toUserCredentials()
    }

    /**
     * Update all fields without phone
     * */
    override suspend fun updateUser(user: User): User? {
        return UserEntity.findSingleByAndUpdate(
                UserTable.phone eq user.phone
            ){ userEntity ->
                userEntity.username = user.username
                userEntity.avatar = user.avatarPath
            }?.toUser()
    }




}