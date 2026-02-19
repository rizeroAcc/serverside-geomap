package com.mapprjct.kotest.datatype

import com.mapprjct.model.datatype.RussiaPhoneNumber
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RussiaPhoneNumberTest : FunSpec() {
    init {
        context("validation check"){
            test("should create class when number valid"){
                shouldNotThrowAny {
                    val phoneStart8 = RussiaPhoneNumber("89036559989")
                    val phoneStartPlus7 = RussiaPhoneNumber("+79036559989")
                }
            }
            test("should throw IllegalArgumentException if phone is blank"){
                shouldThrowExactly<IllegalArgumentException> {
                    val phone = RussiaPhoneNumber("  \n\t  ")
                }.message shouldBe "Phone number must not be empty"
            }
            test("should throw IllegalArgumentException if phone number is not russia phone"){
                shouldThrowExactly<IllegalArgumentException> {
                    val phone = RussiaPhoneNumber("+98642354364")
                }.message shouldBe "Phone must start with +7 or 8 followed by exactly 10 digits"
            }
        }
    }
}