package com.mapprjct.utils

import java.io.File

fun getOrCreateDirectory(relatePath : String) : File {
    val directory = File(relatePath)
    if (!directory.exists()) {
        if(!directory.mkdirs()){
            throw IllegalStateException("Could not create directory ${directory.absolutePath}")
        }
    }
    return directory
}