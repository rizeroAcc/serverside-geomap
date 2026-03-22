package com.mapprjct.builders

import com.mapprjct.model.dto.UserDTO
import com.mapprjct.model.request.auth.RegistrationRequest
import com.mapprjct.model.datatype.Password
import java.util.UUID

fun createRegistrationRequest(block : RegistrationRequestBuilder.()->Unit ): RegistrationRequest {
    return RegistrationRequestBuilder().apply(block).build()
}

class RegistrationRequestBuilder {
    var userDTO : UserDTO? = null
    var password : String = UUID.randomUUID().toString()

    fun forUser(userDTO : UserDTO) {
        this.userDTO = userDTO
    }
    fun withPassword(password: String) {
        this.password = password
    }

    fun build(): RegistrationRequest {
        return RegistrationRequest(
            phone = userDTO?.phone ?: throw NullPointerException("User is null"),
            username = userDTO?.username ?: throw NullPointerException("User is null"),
            password = Password(password)
        )
    }
}