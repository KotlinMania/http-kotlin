// port-lint: source response.rs
package io.github.kotlinmania.http

import io.github.kotlinmania.http.header.HeaderMap
import io.github.kotlinmania.http.header.HeaderName
import io.github.kotlinmania.http.header.HeaderValue
import kotlin.reflect.KClass

/** Head [ResponseParts] and body value extracted via [Response.intoParts]. */
data class ResponseAndParts<T : Any>(
    val parts: ResponseParts,
    val body: T,
)

/**
 * Represents an HTTP response.
 *
 * An HTTP response consists of a head ([ResponseParts]) and a potentially optional body.
 * The body component is generic over any non-null Kotlin type [T], enabling callers to carry
 * byte arrays, strings, streams, or deserialized payloads alongside status and header metadata.
 */
class Response<T : Any> internal constructor(
    private var head: ResponseParts,
    private var bodyValue: T,
) {
    /** Returns the [StatusCode]. */
    fun status(): StatusCode = head.status

    /** Replaces the [StatusCode]. */
    fun setStatus(status: StatusCode) {
        head.status = status
    }

    /** Returns the associated HTTP version. */
    fun version(): Version = head.version

    /** Replaces the associated HTTP version. */
    fun setVersion(version: Version) {
        head.version = version
    }

    /** Returns the associated header field map. */
    fun headers(): HeaderMap<HeaderValue> = head.headers

    /** Returns the mutable header field map for in-place modification. */
    fun headersMut(): HeaderMap<HeaderValue> = head.headers

    /** Returns the associated protocol extensions. */
    fun extensions(): Extensions = head.extensions

    /** Returns the mutable protocol extensions map for in-place modification. */
    fun extensionsMut(): Extensions = head.extensions

    /** Returns the associated HTTP body. */
    fun body(): T = bodyValue

    /** Replaces the associated HTTP body. */
    fun setBody(body: T) {
        bodyValue = body
    }

    /** Consumes the response, returning just the body. */
    fun intoBody(): T = bodyValue

    /** Consumes the response, returning its [ResponseParts] and body. */
    fun intoParts(): ResponseAndParts<T> = ResponseAndParts(head, bodyValue)

    /** Consumes the response, transforming its body with [transform] while keeping the head. */
    fun <U : Any> map(transform: (T) -> U): Response<U> = Response(head, transform(bodyValue))

    /** Returns a copy with independently cloned [ResponseParts] and the current body reference. */
    fun clone(): Response<T> = Response(head.clone(), bodyValue)

    /** Formats a debug representation matching `Response { ... }` (omitting extensions). */
    fun debugString(): String =
        "Response { status: ${head.status.debugString()}, version: ${head.version}, " +
            "headers: ${head.headers.debugString()}, body: $bodyValue }"

    override fun toString(): String = debugString()

    companion object {
        /** Creates a new builder-style object to manufacture a [Response]. */
        fun builder(): ResponseBuilder = ResponseBuilder.new()

        /** Creates a new blank [Response] with [Unit] body and default head parts. */
        fun default(): Response<Unit> = newEmpty()

        /** Creates a new blank [Response] with the given [body]. */
        fun <T : Any> new(body: T): Response<T> = Response(head = ResponseParts.new(), bodyValue = body)

        /** Creates a new blank [Response] with an empty [Unit] body (Swift-friendly). */
        fun newEmpty(): Response<Unit> = new(Unit)

        /** Creates a new blank [Response] with a UTF-8 [String] body (Swift-friendly). */
        fun newText(body: String): Response<String> = new(body)

        /** Creates a new blank [Response] with a raw [ByteArray] body (Swift-friendly). */
        fun newBytes(body: ByteArray): Response<ByteArray> = new(body)

        /** Creates a new [Response] with the given head [parts] and [body]. */
        fun <T : Any> fromParts(
            parts: ResponseParts,
            body: T,
        ): Response<T> = Response(head = parts, bodyValue = body)

        /** Creates a new [Response] with the given [parts] and [String] body (Swift-friendly). */
        fun fromPartsText(
            parts: ResponseParts,
            body: String,
        ): Response<String> = fromParts(parts, body)
    }
}

/**
 * Component parts of an HTTP [Response].
 *
 * The HTTP response head consists of a status, version, a set of header fields,
 * and protocol extensions.
 */
class ResponseParts internal constructor(
    /** The response's status. */
    var status: StatusCode,
    /** The response's version. */
    var version: Version,
    /** The response's headers. */
    val headers: HeaderMap<HeaderValue>,
    /** The response's extensions. */
    var extensions: Extensions,
) {
    /** Clones all head fields into an independent [ResponseParts] instance. */
    fun clone(): ResponseParts =
        ResponseParts(
            status = status.clone(),
            version = version.clone(),
            headers = headers.clone(),
            extensions = extensions.clone(),
        )

    /** Formats a debug representation matching `Parts { ... }`. */
    fun debugString(): String =
        "Parts { status: ${status.debugString()}, version: $version, headers: ${headers.debugString()} }"

    override fun toString(): String = debugString()

    companion object {
        internal fun new(): ResponseParts =
            ResponseParts(
                status = StatusCode.default(),
                version = Version.default(),
                headers = HeaderMap.default(),
                extensions = Extensions.default(),
            )
    }
}

/**
 * An HTTP response builder.
 *
 * This type constructs an instance of [Response] through a builder-style pattern,
 * accumulating parse or header-capacity errors until [body] is called.
 */
class ResponseBuilder private constructor(
    private val inner: HttpResult<ResponseParts>,
) {
    /** Creates a new default instance of [ResponseBuilder]. */
    constructor() : this(HttpResult.ok(ResponseParts.new()))

    /** Sets the HTTP status for this response. Defaults to `200`. */
    fun status(status: StatusCode): ResponseBuilder =
        andThen { head ->
            head.status = status
            HttpResult.ok(head)
        }

    /** Parses and sets the HTTP status from a numeric code. */
    fun status(status: Int): ResponseBuilder = parseStatus(StatusCode.tryFrom(status))

    /** Parses and sets the HTTP status from a string representation. */
    fun status(status: String): ResponseBuilder = parseStatus(StatusCode.tryFrom(status))

    /** Parses and sets the HTTP status from raw ASCII digits. */
    fun status(status: ByteArray): ResponseBuilder = parseStatus(StatusCode.fromBytes(status))

    /** Sets the HTTP version for this response. Defaults to `HTTP/1.1`. */
    fun version(version: Version): ResponseBuilder =
        andThen { head ->
            head.version = version
            HttpResult.ok(head)
        }

    /** Appends a header with [HeaderName] key and [HeaderValue] value. */
    fun header(
        key: HeaderName,
        value: HeaderValue,
    ): ResponseBuilder = appendWithName(key) { Result.success(value) }

    /** Appends a header with [HeaderName] key and parsed string [value]. */
    fun header(
        key: HeaderName,
        value: String,
    ): ResponseBuilder = appendWithName(key) { HeaderValue.fromStr(value) }

    /** Appends a header with [HeaderName] key and parsed [ByteArray] [value]. */
    fun header(
        key: HeaderName,
        value: ByteArray,
    ): ResponseBuilder = appendWithName(key) { HeaderValue.fromBytes(value) }

    /** Appends a header with [HeaderName] key and decimal integer [value]. */
    fun header(
        key: HeaderName,
        value: Int,
    ): ResponseBuilder = appendWithName(key) { HeaderValue.fromStr(value.toString()) }

    /** Appends a header with [HeaderName] key and decimal long [value]. */
    fun header(
        key: HeaderName,
        value: Long,
    ): ResponseBuilder = appendWithName(key) { HeaderValue.fromStr(value.toString()) }

    /** Appends a header with parsed string [key] and [HeaderValue] [value]. */
    fun header(
        key: String,
        value: HeaderValue,
    ): ResponseBuilder = appendWithKeyString(key) { Result.success(value) }

    /** Appends a header with parsed string [key] and string [value]. */
    fun header(
        key: String,
        value: String,
    ): ResponseBuilder = appendWithKeyString(key) { HeaderValue.fromStr(value) }

    /** Appends a header with parsed string [key] and [ByteArray] [value]. */
    fun header(
        key: String,
        value: ByteArray,
    ): ResponseBuilder = appendWithKeyString(key) { HeaderValue.fromBytes(value) }

    /** Appends a header with parsed string [key] and decimal integer [value]. */
    fun header(
        key: String,
        value: Int,
    ): ResponseBuilder = appendWithKeyString(key) { HeaderValue.fromStr(value.toString()) }

    /** Appends a header with parsed string [key] and decimal long [value]. */
    fun header(
        key: String,
        value: Long,
    ): ResponseBuilder = appendWithKeyString(key) { HeaderValue.fromStr(value.toString()) }

    /** Returns the configured headers, or null if the builder has failed. */
    fun headersRef(): HeaderMap<HeaderValue>? = inner.value?.headers

    /** Returns mutable access to the configured headers, or null if the builder has failed. */
    fun headersMut(): HeaderMap<HeaderValue>? = inner.value?.headers

    /** Adds an extension value to this builder. */
    inline fun <reified T : Any> extension(extension: T): ResponseBuilder =
        extension(
            type = T::class,
            typeName = T::class.simpleName ?: T::class.toString(),
            value = extension,
        ) { it }

    /** Adds a typed extension entry using an explicit [KClass], [typeName], and [clone] callback. */
    fun extension(
        type: KClass<*>,
        typeName: String,
        value: Any,
        clone: (Any) -> Any,
    ): ResponseBuilder =
        andThen { head ->
            head.extensions.insert(type, typeName, value, clone)
            HttpResult.ok(head)
        }

    /** Returns the extensions map configured on this builder, or null if the builder has failed. */
    fun extensionsRef(): Extensions? = inner.value?.extensions

    /** Returns mutable access to the extensions map, or null if the builder has failed. */
    fun extensionsMut(): Extensions? = inner.value?.extensions

    /** Consumes this builder and builds a [Response] with the given [body], or returns the accumulated [Error]. */
    fun <T : Any> body(body: T): HttpResult<Response<T>> = inner.map { head -> Response(head = head, bodyValue = body) }

    /** Consumes this builder and builds a [Response] with a [Unit] body (Swift-friendly). */
    fun bodyEmpty(): HttpResult<Response<Unit>> = body(Unit)

    /** Consumes this builder and builds a [Response] with a [String] body (Swift-friendly). */
    fun bodyText(body: String): HttpResult<Response<String>> = body(body)

    /** Consumes this builder and builds a [Response] with a [ByteArray] body (Swift-friendly). */
    fun bodyBytes(body: ByteArray): HttpResult<Response<ByteArray>> = body(body)

    /** Formats a debug representation matching `Builder { inner: ... }`. */
    fun debugString(): String {
        val state = inner.value?.debugString() ?: inner.error?.debugString()
        val label = if (inner.isOk()) "Ok" else "Err"
        return "Builder { inner: $label($state) }"
    }

    override fun toString(): String = debugString()

    private fun parseStatus(result: Result<StatusCode>): ResponseBuilder =
        andThen { head ->
            result.fold(
                onSuccess = { parsed ->
                    head.status = parsed
                    HttpResult.ok(head)
                },
                onFailure = { err -> HttpResult.err(errorFromThrowable(err)) },
            )
        }

    private inline fun appendWithName(
        key: HeaderName,
        crossinline makeValue: () -> Result<HeaderValue>,
    ): ResponseBuilder =
        andThen { head ->
            val err = appendHeaderValue(head.headers, key, makeValue)
            if (err == null) HttpResult.ok(head) else HttpResult.err(err)
        }

    private inline fun appendWithKeyString(
        key: String,
        crossinline makeValue: () -> Result<HeaderValue>,
    ): ResponseBuilder =
        andThen { head ->
            val err = appendHeaderNameAndValue(head.headers, key, makeValue)
            if (err == null) HttpResult.ok(head) else HttpResult.err(err)
        }

    private fun andThen(func: (ResponseParts) -> HttpResult<ResponseParts>): ResponseBuilder =
        ResponseBuilder(inner = inner.andThen(func))

    companion object {
        /** Creates a new default [ResponseBuilder]. */
        fun new(): ResponseBuilder = ResponseBuilder()

        /** Alias for [new] matching Rust `Default::default`. */
        fun default(): ResponseBuilder = ResponseBuilder()
    }
}
