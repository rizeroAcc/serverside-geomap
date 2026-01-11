package com.mapprjct

class AppConfig(
    val databaseURL : String,
    val databaseUsername : String,
    val databasePassword : String,
    val avatarResourcePath : String,
) {
    companion object {
        val Test = AppConfig(
            databaseURL = "",
            databaseUsername = "",
            databasePassword = "",
            avatarResourcePath = "test/api/uploads/avatars/"
        )
    }
}