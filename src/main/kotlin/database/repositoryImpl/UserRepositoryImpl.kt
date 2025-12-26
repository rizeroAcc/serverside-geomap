package com.mapprjct.database.daoimpl

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
                phone = it[UserTable.phone].value,
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
                phone = it[UserTable.phone].value ,
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