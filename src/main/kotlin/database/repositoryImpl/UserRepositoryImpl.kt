package com.mapprjct.database.repositoryImpl

import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.tables.UserTable
import com.mapprjct.model.dto.UserCredentials
import com.mapprjct.model.dto.User
import com.mapprjct.model.datatype.Password
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.Username
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.updateReturning

class UserRepositoryImpl(val database: Database) : UserRepository {

    override suspend fun insert(user: User, password : Password){
        UserTable.insert {
            it[phone] = user.phone.normalizeAsRussiaPhone()
            it[username] = user.username.value
            it[passwordHash] = password.value
            it[avatar] = user.avatarFilename
        }
    }

    override suspend fun getUser(phone : RussiaPhoneNumber) : User?{
        return UserTable
                .selectAll()
                .where {UserTable.phone eq phone.normalizeAsRussiaPhone()}
                .singleOrNull()?.toUser()
    }

    override suspend fun getUserCredentials(phone: RussiaPhoneNumber): UserCredentials? {
        return UserTable
            .selectAll()
            .where { UserTable.phone eq phone.normalizeAsRussiaPhone() }
            .singleOrNull()?.let {
            UserCredentials(
                phone = RussiaPhoneNumber(it[UserTable.phone]),
                password = Password(it[UserTable.passwordHash])
            )
        }
    }


    override suspend fun updateUserPassword(userPhone : RussiaPhoneNumber, password: Password): Int {
        return UserTable.update(
            where = { UserTable.phone eq userPhone.normalizeAsRussiaPhone() }
        ) {
            it[UserTable.passwordHash] = password.value
        }
    }

    /**
     * Update all fields without phone and return updated user
     * */
    override suspend fun updateUser(user: User): User? {
        return UserTable
            .updateReturning(
                returning = UserTable.columns,
                where = { UserTable.phone eq user.phone.value },
                body = {
                    //in future can be more fields
                    it[UserTable.username] = user.username.value
                    it[UserTable.avatar] = user.avatarFilename
                }
            ).singleOrNull()?.toUser()
    }
}

private fun ResultRow.toUser(): User {
    return User(
        phone = RussiaPhoneNumber(this[UserTable.phone]),
        username = Username(this[UserTable.username]),
        avatarFilename = this[UserTable.avatar]
    )
}