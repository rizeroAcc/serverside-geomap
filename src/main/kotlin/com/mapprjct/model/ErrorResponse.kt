package com.mapprjct.model

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
data class ErrorResponse(
    val message : String,
    val details: String? = null,
    val timestamp : Long,
    val errorID : String,
){
    companion object{
        @OptIn(ExperimentalTime::class)
        fun fromText(message : String) : ErrorResponse{
            return ErrorResponse(
                message = message,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                errorID = UUID.randomUUID().toString()
            )
        }
    }
}