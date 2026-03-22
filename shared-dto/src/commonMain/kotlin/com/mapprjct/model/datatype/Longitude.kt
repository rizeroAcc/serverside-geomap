package com.mapprjct.model.datatype

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Longitude(val value : Double) {
    init {
        require(value >= 0.0)
        require(value <= 180.0)
    }
}