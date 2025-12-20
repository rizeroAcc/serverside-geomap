package com.mapprjct.database.daoimpl

import com.mapprjct.database.dao.UserDAO
import com.mapprjct.database.tables.UserTable
import com.mapprjct.dto.Avatar
import com.mapprjct.dto.UserCredentials
import com.mapprjct.dto.User
import com.mapprjct.truncatePhone
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class UserDAOImpl(val database: Database) : UserDAO {
    override suspend fun insert(userCredentials : UserCredentials, username : String) : Int{
        try {
            transaction(database) {
                UserTable.insert {
                    it[phone] = userCredentials.phone.truncatePhone()
                    it[UserTable.username] = username
                    it[passwordHash] = userCredentials.password
                }
            }
            return 1
        }catch (e : Exception) {
            return 0
        }
    }

    override suspend fun getUser(phone : String) : User?{
        val truncatedPhone = phone.truncatePhone()
        return transaction(database) {
            UserTable.selectAll().where {UserTable.phone eq truncatedPhone}.singleOrNull()
        }?.let {
            User(
               phone = "8" + it[UserTable.phone],
                username = it[UserTable.username],
                avatarPath = it[UserTable.avatar],
           )
        }
    }

    override suspend fun getUserCredentials(phone: String): UserCredentials? {
        val truncatedPhone = phone.truncatePhone()
        return transaction(database) {
            UserTable.selectAll().where { UserTable.phone eq truncatedPhone }.singleOrNull()
        }?.let {
            UserCredentials(
                phone = it[UserTable.phone] ,
                password = it[UserTable.passwordHash]
            )
        }
    }

    override suspend fun updateUserAvatar(user : User,avatar: Avatar): User? {
        return try {
            transaction(database) {
                UserTable.update(where = {
                    UserTable.phone eq user.phone
                }, body = {
                    it[UserTable.avatar] = avatar.path
                })
            }
            user.copy(avatarPath = avatar.path)
        }catch (e : Exception){
            e.printStackTrace()
            null
        }

    }

    override suspend fun updateUserCredentials(userPhone : String, newCredentials : UserCredentials): UserCredentials? {
        transaction(database) {
            UserTable.update {
                it[UserTable.passwordHash]=newCredentials.password
            }
        }
        return newCredentials
    }


}