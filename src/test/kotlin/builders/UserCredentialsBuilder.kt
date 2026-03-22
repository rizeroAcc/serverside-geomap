package com.mapprjct.builders

import com.mapprjct.model.dto.UserDTO
import com.mapprjct.model.dto.UserCredentialsDTO
import com.mapprjct.model.datatype.Password
import com.mapprjct.model.datatype.RussiaPhoneNumber

fun createCredentials(block : UserCredentialsBuilder.()->Unit): UserCredentialsDTO {
    return UserCredentialsBuilder().apply(block).build()
}

class UserCredentialsBuilder {
    var phone : String? = null
    var password : String? = null
    fun build(): UserCredentialsDTO {
        if (phone == null) {
            throw NullPointerException("phone is null")
        }
        if (password == null) {
            throw NullPointerException("password is null")
        }
        return UserCredentialsDTO(RussiaPhoneNumber(phone!!), Password(password!!))
    }
    fun forUser(userDTO: UserDTO) {
        phone = userDTO.phone.value
    }
}