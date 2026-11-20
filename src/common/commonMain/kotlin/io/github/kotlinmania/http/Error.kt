// port-lint: source error.rs
package io.github.kotlinmania.http

import io.github.kotlinmania.http.header.InvalidHeaderName
import io.github.kotlinmania.http.header.InvalidHeaderValue
import io.github.kotlinmania.http.uri.InvalidUri
import io.github.kotlinmania.http.uri.InvalidUriParts
import kotlin.reflect.KClass
import io.github.kotlinmania.http.header.MaxSizeReached as HeaderMaxSizeReached

/**
 * A generic "error" for HTTP connections.
 *
 * This error type is less specific than the error returned from other
 * functions in this crate, but all other errors can be converted to this
 * error. Consumers of this crate can typically consume and work with this form
 * of error for conversions with [kotlin.Result].
 */
class Error internal constructor(
    internal val inner: ErrorKind,
) {
    /** Human-readable error message matching the underlying HTTP error. */
    val message: String get() = inner.describe()

    /** Returns the internal error variant wrapper. */
    internal fun kind(): ErrorKind = inner

    /** Returns true if the underlying error has the same runtime type as [T]. */
    inline fun <reified T : Any> isType(): Boolean = getRef() is T

    /** Returns true if the underlying error is an instance of [type]. */
    fun isError(type: KClass<*>): Boolean = type.isInstance(getRef())

    /** Returns a reference to the lower-level inner error. */
    fun getRef(): Any =
        when (val kind = inner) {
            is ErrorKind.StatusCode -> kind.error
            is ErrorKind.Method -> kind.error
            is ErrorKind.Uri -> kind.error
            is ErrorKind.UriParts -> kind.error
            is ErrorKind.HeaderName -> kind.error
            is ErrorKind.HeaderValue -> kind.error
            is ErrorKind.MaxSizeReached -> kind.error
        }

    /**
     * Returns any available cause from the inner error.
     *
     * Note the inner error is not itself the cause.
     */
    fun source(): Error? = null

    /** Formats a debug representation matching `http::Error(...)`. */
    fun debugString(): String = "http::Error(${inner.debugDescribe()})"

    override fun toString(): String = inner.describe()

    override fun equals(other: Any?): Boolean = other is Error && inner == other.inner

    override fun hashCode(): Int = inner.hashCode()

    companion object {
        fun from(err: HeaderMaxSizeReached): Error = Error(ErrorKind.MaxSizeReached(err))

        fun from(err: InvalidStatusCode): Error = Error(ErrorKind.StatusCode(err))

        fun from(err: InvalidMethod): Error = Error(ErrorKind.Method(err))

        fun from(err: InvalidUri): Error = Error(ErrorKind.Uri(err))

        fun from(err: InvalidUriParts): Error = Error(ErrorKind.UriParts(err))

        fun from(err: InvalidHeaderName): Error = Error(ErrorKind.HeaderName(err))

        fun from(err: InvalidHeaderValue): Error = Error(ErrorKind.HeaderValue(err))
    }
}

/** Result alias matching Rust `http::Result<T>` without exposing `Throwable` to Swift Export. */
class HttpResult<T : Any> private constructor(
    val value: T?,
    val error: Error?,
) {
    val isSuccess: Boolean get() = error == null
    val isFailure: Boolean get() = error != null

    /** Returns true if this result holds a success value. */
    fun isOk(): Boolean = error == null

    /** Returns true if this result holds an [Error]. */
    fun isErr(): Boolean = error != null

    /** Returns the contained value, or null if failed. */
    fun ok(): T? = value

    /** Returns the contained [Error], or null if succeeded. */
    fun err(): Error? = error

    /** Returns the contained value, throwing [IllegalStateException] if failed. */
    fun unwrap(): T = value ?: error(error?.message ?: "HttpResult failure")

    /** Alias for [unwrap] matching Kotlin `Result.getOrThrow`. */
    fun getOrThrow(): T = unwrap()

    /** Returns the contained value, or null if failed. */
    fun getOrNull(): T? = value

    /** Returns the contained [Error], or null if succeeded. */
    fun errorOrNull(): Error? = error

    /** Transforms the contained success value with [transform], preserving any failure. */
    fun <R : Any> map(transform: (T) -> R): HttpResult<R> {
        val current = value
        return if (error == null && current != null) {
            ok(transform(current))
        } else {
            err(checkNotNull(error))
        }
    }

    /** Chains another fallible computation returning [HttpResult], preserving any prior failure. */
    fun <R : Any> andThen(transform: (T) -> HttpResult<R>): HttpResult<R> {
        val current = value
        return if (error == null && current != null) {
            transform(current)
        } else {
            err(checkNotNull(error))
        }
    }

    override fun equals(other: Any?): Boolean =
        other is HttpResult<*> && value == other.value && error == other.error

    override fun hashCode(): Int = 31 * (value?.hashCode() ?: 0) + (error?.hashCode() ?: 0)

    override fun toString(): String =
        if (error == null) {
            "Ok($value)"
        } else {
            "Err($error)"
        }

    companion object {
        fun <T : Any> ok(value: T): HttpResult<T> = HttpResult(value = value, error = null)

        fun <T : Any> err(error: Error): HttpResult<T> = HttpResult(value = null, error = error)
    }
}

internal fun errorFromThrowable(err: Throwable): Error =
    when (err) {
        is HeaderMaxSizeReached -> Error.from(err)
        is InvalidStatusCode -> Error.from(err)
        is InvalidMethod -> Error.from(err)
        is InvalidUri -> Error.from(err)
        is InvalidUriParts -> Error.from(err)
        is InvalidHeaderValue -> Error.from(err)
        else -> error("unexpected HTTP throwable: $err")
    }

/** Sealed hierarchy representing the concrete inner error kind inside [Error]. */
internal sealed class ErrorKind {
    data class StatusCode(
        val error: InvalidStatusCode,
    ) : ErrorKind()

    data class Method(
        val error: InvalidMethod,
    ) : ErrorKind()

    data class Uri(
        val error: InvalidUri,
    ) : ErrorKind()

    data class UriParts(
        val error: InvalidUriParts,
    ) : ErrorKind()

    data class HeaderName(
        val error: InvalidHeaderName,
    ) : ErrorKind()

    data class HeaderValue(
        val error: InvalidHeaderValue,
    ) : ErrorKind()

    data class MaxSizeReached(
        val error: HeaderMaxSizeReached,
    ) : ErrorKind()

    internal fun describe(): String =
        when (this) {
            is StatusCode -> error.toString()
            is Method -> error.message ?: error.toString()
            is Uri -> error.message ?: "invalid URI"
            is UriParts -> error.message ?: "invalid URI"
            is HeaderName -> error.message
            is HeaderValue -> error.toString()
            is MaxSizeReached -> error.toString()
        }

    internal fun debugDescribe(): String =
        when (this) {
            is StatusCode -> error.fmt()
            is Method -> error.fmt()
            is Uri -> "InvalidUri(${error.kind})"
            is UriParts -> "InvalidUriParts(InvalidUri(${error.inner.kind}))"
            is HeaderName -> error.debugString()
            is HeaderValue -> error.debugString()
            is MaxSizeReached -> error.debugString()
        }
}
