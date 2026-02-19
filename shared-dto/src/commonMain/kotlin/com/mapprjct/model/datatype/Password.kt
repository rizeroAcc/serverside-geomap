package com.mapprjct.model.datatype

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class Password(val value : String) {
    init {
        require(value.isNotBlank()) { "Password must not be blank." }
        require(value.length >= 8) { "Password must have at least 8 characters long." }
    }
}