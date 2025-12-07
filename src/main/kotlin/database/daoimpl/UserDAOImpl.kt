package com.mapprjct.database.daoimpl

import com.mapprjct.database.dao.UserDAO
import com.mapprjct.database.tables.UserTable
import com.mapprjct.dto.UserCredentials
import com.mapprjct.dto.User
import com.mapprjct.truncatePhone
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class UserDAOImpl(val database: Database) : UserDAO {
    override suspend fun insert(user : UserCredentials) : Int{
        try {
            transaction(database) {
                UserTable.insert {
                    it[phone] = user.phone.truncatePhone()
                    it[username] = user.username
                    it[passwordHash] = user.passwordHash
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
               phone = it[UserTable.phone],
                username = it[UserTable.username],
                avatar = it[UserTable.avatar],
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
                username = it[UserTable.username] ,
                passwordHash = it[UserTable.passwordHash]
            )
        }
    }

    override suspend fun updateUser(user: User): User? {
        return try {
            transaction(database) {
                UserTable.update {
                    if (user.username.isNotEmpty()) {
                        it[username] = user.username
                    }
                    it[avatar] = user.avatar
                }
            }
            user
        }catch (e : Exception){
            e.printStackTrace()
            null
        }

    }
}