package com.mapprjct.kotest.datatype

import com.mapprjct.model.value.StringUUID
import io.kotest.core.spec.style.FunSpec
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class StringUUIDTest : FunSpec(){
    init {
        context("validation test") {
            test("should create valid UUID string") {
                val id = StringUUID(Uuid.random().toString())
            }
        }
    }
}