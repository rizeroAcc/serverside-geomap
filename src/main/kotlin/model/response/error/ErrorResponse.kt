package com.mapprjct.model.response.error

import com.mapprjct.exceptions.BaseAppException
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
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

        @OptIn(ExperimentalTime::class)
        fun fromThrowable(throwable : Throwable, whatsHappened : String) : ErrorResponse{
            return ErrorResponse(
                message = whatsHappened,
                detailedMessage = whatsHappened,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                errorID = UUID.randomUUID().toString()
            )
        }
    }
}