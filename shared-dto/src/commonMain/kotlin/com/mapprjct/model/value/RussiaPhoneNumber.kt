package com.mapprjct.model.value

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline



@JvmInline
@Serializable
value class RussiaPhoneNumber(val value : String) {
    init {
        require(value.isNotBlank()) { "Phone number must not be empty" }
        require(value.matches(Regex("^(\\+7|8)\\d{10}$"))) { "Phone must start with +7 or 8 followed by exactly 10 digits" }
    }
    fun normalizeAsRussiaPhone(): String =
        if (value.startsWith("8")) "+7${value.drop(1)}" else value
}