package com.mapprjct.utils

import com.mapprjct.utils.DatabaseDataResult.Companion.databaseError
import com.mapprjct.utils.DatabaseDataResult.Companion.domainError
import com.mapprjct.utils.DatabaseDataResult.Companion.success
import com.mapprjct.utils.DatabaseDataResult.Companion.unexpectedError
import io.ktor.utils.io.CancellationException
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.rmi.UnexpectedException

sealed interface DatabaseDataResult<out S, out F> {
    class Success<out S>(val result : S) : DatabaseDataResult<S, Nothing>
    sealed class Failure<out F> : DatabaseDataResult<Nothing,F>{
        class DomainError<out F>(val exception: F) : Failure<F>()
        class DatabaseError(val exception: ExposedSQLException) : Failure<Nothing>()
        class UnexpectedException(val exception: Throwable) : Failure<Nothing>()
    }


    companion object {
        fun<S> success(result : S) = Success(result)
        fun<F> domainError(exception: F) = Failure.DomainError(exception)
        fun databaseError(exception: ExposedSQLException) = Failure.DatabaseError(exception)

        fun unexpectedError(exception: Throwable) = Failure.UnexpectedException(exception)
    }
}

suspend inline fun<S, reified F> accessDatabaseData(
    database : Database,
    noinline databaseExceptionMapper: ((exception : ExposedSQLException) -> DatabaseDataResult.Failure<F>)? = null,
    crossinline block: suspend JdbcTransaction.() -> S
) : DatabaseDataResult<S,F>{
    return try {
        val result = suspendTransaction(database) {
            block()
        }
        success(result)
    }catch (e : ExposedSQLException){
        if (databaseExceptionMapper != null){
            val mappedError = databaseExceptionMapper(e)
            when (mappedError){
                is DatabaseDataResult.Failure.DatabaseError -> TODO()
                is DatabaseDataResult.Failure.DomainError<*> -> TODO()
                is DatabaseDataResult.Failure.UnexpectedException -> TODO()
            }
        }else{
            databaseError(e)
        }

    }catch (e : CancellationException){
        throw e
    }catch (e : Throwable){
        if (e is F){
            domainError(e)
        } else {
            unexpectedError(e)
        }
    }
}

