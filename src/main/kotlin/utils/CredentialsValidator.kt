package com.mapprjct.utils

class CredentialsValidator private constructor() {
    companion object {
        fun validatePhone(phone : String) : Boolean{
            return if (phone.startsWith("8")) {
                phone.length == 11
            }else if(phone.startsWith("+7")) {
                phone.length == 12
            }else {
                false
            }
        }

        fun validatePassword(password : String) : Boolean {
            return password.length >=8
        }
    }
}