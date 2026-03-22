package com.mapprjct.model.datatype

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline



@JvmInline
@Serializable
value class RussiaPhoneNumber private constructor(val value : String) {
    fun normalizeAsRussiaPhone(): String =
        if (value.startsWith("8")) "+7${value.drop(1)}" else value

    companion object {
        operator fun invoke(value: String) : RussiaPhoneNumber {
            require(value.isNotBlank()) { "Phone number must not be empty" }
            require(value.matches(Regex("^(\\+7|8)\\d{10}$"))) { "Phone must start with +7 or 8 followed by exactly 10 digits" }
            return RussiaPhoneNumber(value)
        }
    }
}