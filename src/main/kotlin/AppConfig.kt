package com.mapprjct

class DatabaseConfig(
    val url : String,
    val username : String,
    val password : String,
)
class MinioConfig(
    val endpoint : String,
    val accessKey : String,
    val secretKey : String,
    val placemarkIconBucketName : String,
)

class AppConfig(
    val database : DatabaseConfig,
    val minio : MinioConfig,
    val avatarResourcePath : String,
    val placemarkIconsPath : String,
)