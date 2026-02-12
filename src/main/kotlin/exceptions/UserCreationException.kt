package com.mapprjct.exceptions

sealed class UserCreationException {
    class UserAlreadyExists : UserCreationException()
}