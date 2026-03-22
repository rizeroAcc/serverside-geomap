package com.mapprjct.database.repositoryImpl

import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.tables.UserTable
import com.mapprjct.model.dto.UserCredentialsDTO
import com.mapprjct.model.dto.UserDTO
import com.mapprjct.model.datatype.Password
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.Username
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.updateReturning

class UserRepositoryImpl(val database: Database) : UserRepository {

    override suspend fun insert(userDTO: UserDTO, password : Password) : UserDTO{
        return UserTable.insertReturning(
            returning = UserTable.columns
        ) {
            it[phone] = userDTO.phone.normalizeAsRussiaPhone()
            it[username] = userDTO.username.value
            it[passwordHash] = password.value
            it[avatar] = userDTO.avatarFilename
        }.single().toUser()
    }

    override suspend fun findUser(phone : RussiaPhoneNumber) : UserDTO?{
        return UserTable
                .selectAll()
                .where {UserTable.phone eq phone.normalizeAsRussiaPhone()}
                .singleOrNull()?.toUser()
    }

    override suspend fun getUserCredentials(phone: RussiaPhoneNumber): UserCredentialsDTO? {
        return UserTable
            .selectAll()
            .where { UserTable.phone eq phone.normalizeAsRussiaPhone() }
            .singleOrNull()?.let {
            UserCredentialsDTO(
                phone = RussiaPhoneNumber(it[UserTable.phone]),
                password = Password(it[UserTable.passwordHash])
            )
        }
    }


    override suspend fun updateUserPassword(userPhone : RussiaPhoneNumber, password: Password): Password? {
        return UserTable.updateReturning(
            returning = listOf(UserTable.passwordHash),
            where = { UserTable.phone eq userPhone.normalizeAsRussiaPhone() }
        ) {
            it[UserTable.passwordHash] = password.value
        }.singleOrNull()?.let { resultRow->
            Password(resultRow[UserTable.passwordHash])
        }
    }

    /**
     * Update all fields without phone and return updated user
     * */
    override suspend fun updateUser(userDTO: UserDTO): UserDTO? {
        return UserTable
            .updateReturning(
                returning = UserTable.columns,
                where = { UserTable.phone eq userDTO.phone.value },
                body = {
                    //in future can be more fields
                    it[UserTable.username] = userDTO.username.value
                    it[UserTable.avatar] = userDTO.avatarFilename
                }
            ).singleOrNull()?.toUser()
    }
}

private fun ResultRow.toUser(): UserDTO {
    return UserDTO(
        phone = RussiaPhoneNumber(this[UserTable.phone]),
        username = Username(this[UserTable.username]),
        avatarFilename = this[UserTable.avatar]
    )
}