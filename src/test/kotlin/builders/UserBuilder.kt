package com.mapprjct.builders
import com.mapprjct.model.dto.User

/**
 * Create user with valid phone,username and empty avatar
 * */
fun createTestUser(block: UserBuilder.() -> Unit = {}) =
    UserBuilder().apply(block).build()

class UserBuilder {
    var phone: String = "89036559989"
    var username: String = "testName"
    var avatarFilename: String? = null
    fun build() = User(phone, username, avatarFilename)
}