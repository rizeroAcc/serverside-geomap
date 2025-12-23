package com.mapprjct.database.daoimpl

import com.mapprjct.database.dao.UserDAO
import com.mapprjct.database.tables.UserTable
import com.mapprjct.dto.Avatar
import com.mapprjct.dto.UserCredentials
import com.mapprjct.dto.User
import com.mapprjct.truncatePhone
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertReturning
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class UserDAOImpl(val database: Database) : UserDAO {
    override suspend fun insert(user: User, password : String){
        transaction(database) {
            UserTable.insert {
                it[phone] = user.phone.replaceRussiaCountryCode()
                it[UserTable.username] = user.username
                it[passwordHash] = password
                it[avatar] = user.avatarPath
            }
        }
    }

    override suspend fun getUser(phone : String) : User?{
        return transaction(database) {
            UserTable.selectAll().where {UserTable.phone eq phone.replaceRussiaCountryCode()}.singleOrNull()
        }?.let {
            User(
               phone = it[UserTable.phone],
                username = it[UserTable.username],
                avatarPath = it[UserTable.avatar],
           )
        }
    }

    override suspend fun getUserCredentials(phone: String): UserCredentials? {
        return transaction(database) {
            UserTable.selectAll().where { UserTable.phone eq phone.replaceRussiaCountryCode() }.singleOrNull()
        }?.let {
            UserCredentials(
                phone = it[UserTable.phone] ,
                password = it[UserTable.passwordHash]
            )
        }
    }


    override suspend fun updateUserCredentials(userPhone : String, newCredentials : UserCredentials): Int {
        return transaction(database) {
            UserTable.update {
                it[phone] = newCredentials.phone
                it[UserTable.passwordHash] = newCredentials.password
            }
        }
    }
    
    /**
     * Update all fields without phone
     * */
    override suspend fun updateUser(user: User): Int {
        return transaction(database) {
            UserTable.update(
                where = {
                    UserTable.phone eq user.phone
                },
                body = {
                    //in future can be more fields
                    it[UserTable.username] = user.username
                    it[UserTable.avatar] = user.avatarPath
                }
            )
        }
    }


    /**
     * Change phone signature from +7********** to 8**********
     * If phone already start with 8 don't do anything
     * */
    private fun String.replaceRussiaCountryCode() : String{
        return if (this.startsWith("+7")) this.replace("+7", "8") else this
    }

}