package com.mapprjct.builders

import com.mapprjct.model.dto.User
import com.mapprjct.model.dto.UserCredentials

fun createCredentials(block : UserCredentialsBuilder.()->Unit): UserCredentials {
    return UserCredentialsBuilder().apply(block).build()
}

class UserCredentialsBuilder {
    var phone : String? = null
    var password : String? = null
    fun build(): UserCredentials {
        if (phone == null) {
            throw NullPointerException("phone is null")
        }
        if (password == null) {
            throw NullPointerException("password is null")
        }
        return UserCredentials(phone!!, password!!)
    }
    fun forUser(user: User) {
        phone = user.phone
    }
}