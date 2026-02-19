package com.mapprjct.model.datatype

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class EntityName(val value: String) {
    init {
        require(value.isNotBlank()) { "Name must not be empty." }
    }
}