package com.mapprjct.builders

import com.mapprjct.model.dto.User
import com.mapprjct.model.request.auth.RegistrationRequest
import com.mapprjct.model.value.Password
import java.util.UUID

fun createRegistrationRequest(block : RegistrationRequestBuilder.()->Unit ): RegistrationRequest {
    return RegistrationRequestBuilder().apply(block).build()
}

class RegistrationRequestBuilder {
    var user : User? = null
    var password : String = UUID.randomUUID().toString()

    fun forUser(user : User) {
        this.user = user
    }
    fun withPassword(password: String) {
        this.password = password
    }

    fun build(): RegistrationRequest {
        return RegistrationRequest(
            phone = user?.phone ?: throw NullPointerException("User is null"),
            username = user?.username ?: throw NullPointerException("User is null"),
            password = Password(password)
        )
    }
}