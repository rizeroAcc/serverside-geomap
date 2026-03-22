package com.mapprjct.model.datatype

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Latitude(val value: Double) {
    init {
        require(value >= 0.0)
        require(value <= 90.0)
    }
}