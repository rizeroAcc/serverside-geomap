package com.mapprjct.builders
import com.mapprjct.model.dto.User
import com.mapprjct.model.value.RussiaPhoneNumber
import com.mapprjct.model.value.Username

/**
 * Create user with valid phone,username and empty avatar
 * */
fun createTestUser(block: UserBuilder.() -> Unit = {}) =
    UserBuilder().apply(block).build()

class UserBuilder {
    var phone: String = "89036559989"
    var username: String = "testName"
    var avatarFilename: String? = null
    fun build() = User(RussiaPhoneNumber(phone), Username(username), avatarFilename)
}