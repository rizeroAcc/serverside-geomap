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