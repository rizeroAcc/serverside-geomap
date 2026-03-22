package com.mapprjct.model.datatype

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class EntityName private constructor(val value: String) {
    companion object{
        operator fun invoke(value: String): EntityName {
            require(value.isNotBlank()) { "Name must not be empty." }
            return EntityName(value)
        }
    }
}