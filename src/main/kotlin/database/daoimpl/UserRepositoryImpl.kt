package com.mapprjct.database.daoimpl

import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.tables.UserTable
import com.mapprjct.dto.UserCredentials
import com.mapprjct.dto.User
import com.mapprjct.replaceRussiaCountryCode
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class UserRepositoryImpl(val database: Database) : UserRepository {
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


    override suspend fun updateUserPassword(userPhone : String, password: String): Int {
        return transaction(database) {
            UserTable.update(
                where = { UserTable.phone eq userPhone.replaceRussiaCountryCode() },
            ) {
                it[UserTable.passwordHash] = password
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




}