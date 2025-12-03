package com.mapprjct.database.users

import com.mapprjct.database.users.UserTable.passwordHash
import com.mapprjct.database.users.UserTable.username
import com.mapprjct.database.dao.UserDAO
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class UserDAOImpl(val database: Database) : UserDAO {
    override suspend fun insert(user : UserDTO) : Int{
        try {
            transaction(database) {
                UserTable.insert {
                    it[phone] = user.phone
                    it[username] = user.username
                    it[passwordHash] = user.passwordHash
                }
            }
            return 1
        }catch (e : Exception) {
            return 0
        }

    }

    override suspend fun getUserByPhone(phone : String) : UserDTO?{
        var userModel : ResultRow? = null
        try {
            transaction(database) {
                userModel = UserTable.selectAll().where { UserTable.phone eq phone }.singleOrNull()
            }
        }catch (e: Exception){
            e.printStackTrace()
        }

        return userModel?.let {
            UserDTO(
                phone = it[UserTable.phone],
                username = it[username],
                passwordHash = it[passwordHash]
            )
        }
    }
}