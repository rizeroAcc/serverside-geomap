package com.mapprjct.exceptions.domain

abstract class BaseDomainException : Exception() {
    abstract val shortMessage: String
    abstract val detailedMessage: String
}