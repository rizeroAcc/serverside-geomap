package com.mapprjct.utils

sealed interface Either<out S,out F> {
    data class Success<out S>(val value: S) : Either<S, Nothing>
    data class Failure<out F>(val error : F) : Either<Nothing, F>

    companion object {
        fun <S> success(value: S): Either<S, Nothing> = Success(value)
        fun <F> failure(error: F): Either<Nothing, F> = Failure(error)
    }

}

inline fun <S, F, R> Either<S, F>.fold(
    onSuccess: (S) -> R,
    onError: (F) -> R
): R = when (this) {
    is Either.Success -> onSuccess(value)
    is Either.Failure -> onError(error)
}

inline fun<S,reified F> Result<S>.toEither(exceptionMapper : (Throwable)->F) : Either<S,F> {
    return if(this.isSuccess) {
        Either.success(this.getOrThrow())
    }else{
        val exception = exceptionOrNull()!!
        if (exception is F) {
            Either.failure(exception)
        }else{
            Either.failure(exceptionMapper(this.exceptionOrNull()!!))
        }
    }
}

inline fun<S,F> Either<S,F>.getOrElse(mapper: (F) -> S) : S = when(this){
    is Either.Failure<F> -> mapper(this.error)
    is Either.Success<S> -> this.value
}
