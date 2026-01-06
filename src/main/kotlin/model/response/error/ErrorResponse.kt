package com.mapprjct.model.response.error

import com.mapprjct.exceptions.BaseAppException
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class ErrorResponse(
    val message : String,
    val detailedMessage: String,
    val timestamp : Long,
    val errorID : String,
){
    companion object{
        @OptIn(ExperimentalTime::class)
        fun fromAppException(exception : BaseAppException) : ErrorResponse{
            return ErrorResponse(
                message = exception.shortMessage,
                detailedMessage = exception.detailedMessage,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                errorID = UUID.randomUUID().toString()
            )
        }
        @OptIn(ExperimentalTime::class)
        fun loggedDatabaseException(exception: ExposedSQLException) : ErrorResponse{
            return ErrorResponse(
                message = "Server error",
                detailedMessage = "See logs for more details",
                timestamp = Clock.System.now().toEpochMilliseconds(),
                errorID = UUID.randomUUID().toString()
            )
        }
    }
}