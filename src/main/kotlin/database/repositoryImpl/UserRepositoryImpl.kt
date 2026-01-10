package com.mapprjct.database.repositoryImpl

import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.tables.UserTable
import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.model.dto.User
import com.mapprjct.utils.replaceRussiaCountryCode
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.updateReturning

class UserRepositoryImpl(val database: Database) : UserRepository {

    override suspend fun insert(user: User, password : String){
        UserTable.insert {
            it[phone] = user.phone.replaceRussiaCountryCode()
            it[UserTable.username] = user.username
            it[passwordHash] = password
            it[avatar] = user.avatarFilename
        }
    }

    override suspend fun getUser(phone : String) : User?{
        return UserTable
            .selectAll()
            .where {UserTable.phone eq phone.replaceRussiaCountryCode()}
            .singleOrNull()?.let {
                User(
                    phone = it[UserTable.phone],
                    username = it[UserTable.username],
                    avatarFilename = it[UserTable.avatar],
            )
        }
    }

    override suspend fun getUserCredentials(phone: String): UserCredentials? {
        return UserTable
            .selectAll()
            .where { UserTable.phone eq phone.replaceRussiaCountryCode() }
            .singleOrNull()?.let {
            UserCredentials(
                phone = it[UserTable.phone],
                password = it[UserTable.passwordHash]
            )
        }
    }


    override suspend fun updateUserPassword(userPhone : String, password: String): Int {
        return UserTable.update(
            where = { UserTable.phone eq userPhone.replaceRussiaCountryCode() }
        ) {
            it[UserTable.passwordHash] = password
        }
    }

    /**
     * Update all fields without phone and return updated user
     * */
    override suspend fun updateUser(user: User): User? {
        return UserTable
            .updateReturning(
                returning = UserTable.columns,
                where = { UserTable.phone eq user.phone },
                body = {
                    //in future can be more fields
                    it[UserTable.username] = user.username
                    it[UserTable.avatar] = user.avatarFilename
                }
            ).singleOrNull()?.toUser()
    }
}

private fun ResultRow.toUser(): User {
    return User(
        phone = this[UserTable.phone],
        username = this[UserTable.username],
        avatarFilename = this[UserTable.avatar]
    )
}