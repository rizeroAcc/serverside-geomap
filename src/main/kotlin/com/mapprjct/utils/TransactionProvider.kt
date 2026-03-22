package com.mapprjct.com.mapprjct.utils

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

interface TransactionProvider {
    suspend fun<T> runInTransaction(block: suspend () -> T) : T
}

class SuspendTransactionProvider(val database : Database?) : TransactionProvider {
    override suspend fun <T> runInTransaction(block: suspend () -> T) : T = suspendTransaction(database) { block() }
}

class BypassTransactionProvider() : TransactionProvider {
    override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
}