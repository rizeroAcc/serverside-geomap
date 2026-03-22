package com.mapprjct.exceptions

abstract class BaseAppException() : Exception() {
    abstract val shortMessage: String
    abstract val detailedMessage: String
}