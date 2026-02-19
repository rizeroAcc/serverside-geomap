package com.mapprjct.model.datatype

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Username(val value: String) {
    init {
        require(value.isNotBlank()) { "Username must not be empty." }
    }
}