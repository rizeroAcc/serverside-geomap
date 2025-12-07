package com.mapprjct

fun String.truncatePhone() : String {
    return if (this.startsWith("+7")){
        this.substring(2)
    }else if(startsWith("8")){
        this.substring(1)
    }else {
        throw IllegalStateException("Invalid phone format")
    }
}