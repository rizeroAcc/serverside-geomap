package com.mapprjct.utils

/**
 * Change phone signature from +7********** to 8**********
 * If phone already start with 8 don't do anything
 * */
fun String.replaceRussiaCountryCode() : String{
    return if (this.startsWith("+7")) this.replace("+7", "8") else this
}