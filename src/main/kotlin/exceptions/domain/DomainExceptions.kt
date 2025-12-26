package com.mapprjct.exceptions.domain


class InvalidDataException(
    val shortDescription : String? = null,
    val details : String
) : BaseDomainException() {
    override val shortMessage: String
        get() = shortDescription ?: "Invalid request data"
    override val detailedMessage: String
        get() = details
}