// port-lint: source request.rs
package io.github.kotlinmania.http

import io.github.kotlinmania.http.header.HeaderMap
import io.github.kotlinmania.http.header.HeaderName
import io.github.kotlinmania.http.header.HeaderValue
import io.github.kotlinmania.http.uri.Uri
import kotlin.reflect.KClass

/** Head [RequestParts] and body value extracted via [Request.intoParts]. */
data class RequestAndParts<T : Any>(
    val parts: RequestParts,
    val body: T,
)

/**
 * Represents an HTTP request.
 *
 * An HTTP request consists of a head ([RequestParts]) and a potentially optional body.
 * The body component is generic over any non-null Kotlin type [T], allowing callers to
 * store byte arrays, strings, streams, or domain models alongside HTTP metadata.
 */
class Request<T : Any> internal constructor(
    private var head: RequestParts,
    private var bodyValue: T,
) {
    /** Returns the associated HTTP method. */
    fun method(): Method = head.method

    /** Replaces the associated HTTP method. */
    fun setMethod(method: Method) {
        head.method = method
    }

    /** Returns the associated URI. */
    fun uri(): Uri = head.uri

    /** Replaces the associated URI. */
    fun setUri(uri: Uri) {
        head.uri = uri
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

    /** Consumes the request, returning just the body. */
    fun intoBody(): T = bodyValue

    /** Consumes the request, returning its [RequestParts] and body. */
    fun intoParts(): RequestAndParts<T> = RequestAndParts(head, bodyValue)

    /** Consumes the request, transforming its body with [transform] while keeping the head. */
    fun <U : Any> map(transform: (T) -> U): Request<U> = Request(head, transform(bodyValue))

    /** Returns a copy with independently cloned [RequestParts] and the current body reference. */
    fun clone(): Request<T> = Request(head.clone(), bodyValue)

    /** Formats a debug representation matching `Request { ... }` (omitting extensions). */
    fun debugString(): String =
        "Request { method: ${head.method}, uri: ${head.uri}, version: ${head.version}, " +
            "headers: ${head.headers.debugString()}, body: $bodyValue }"

    override fun toString(): String = debugString()

    companion object {
        /** Creates a new builder-style object to manufacture a [Request]. */
        fun builder(): RequestBuilder = RequestBuilder.new()

        /** Creates a new blank [Request] with [Unit] body and default head parts. */
        fun default(): Request<Unit> = newEmpty()

        /** Creates a [RequestBuilder] initialized with a `GET` method and [uri]. */
        fun get(uri: Uri): RequestBuilder = methodWithUri(Method.GET, uri)

        /** Creates a [RequestBuilder] initialized with a `GET` method and parsed [uri]. */
        fun get(uri: String): RequestBuilder = methodWithUri(Method.GET, uri)

        /** Creates a [RequestBuilder] initialized with a `GET` method and parsed [uri] bytes. */
        fun get(uri: ByteArray): RequestBuilder = methodWithUri(Method.GET, uri)

        /** Creates a [RequestBuilder] initialized with a `PUT` method and [uri]. */
        fun put(uri: Uri): RequestBuilder = methodWithUri(Method.PUT, uri)

        /** Creates a [RequestBuilder] initialized with a `PUT` method and parsed [uri]. */
        fun put(uri: String): RequestBuilder = methodWithUri(Method.PUT, uri)

        /** Creates a [RequestBuilder] initialized with a `PUT` method and parsed [uri] bytes. */
        fun put(uri: ByteArray): RequestBuilder = methodWithUri(Method.PUT, uri)

        /** Creates a [RequestBuilder] initialized with a `POST` method and [uri]. */
        fun post(uri: Uri): RequestBuilder = methodWithUri(Method.POST, uri)

        /** Creates a [RequestBuilder] initialized with a `POST` method and parsed [uri]. */
        fun post(uri: String): RequestBuilder = methodWithUri(Method.POST, uri)

        /** Creates a [RequestBuilder] initialized with a `POST` method and parsed [uri] bytes. */
        fun post(uri: ByteArray): RequestBuilder = methodWithUri(Method.POST, uri)

        /** Creates a [RequestBuilder] initialized with a `DELETE` method and [uri]. */
        fun delete(uri: Uri): RequestBuilder = methodWithUri(Method.DELETE, uri)

        /** Creates a [RequestBuilder] initialized with a `DELETE` method and parsed [uri]. */
        fun delete(uri: String): RequestBuilder = methodWithUri(Method.DELETE, uri)

        /** Creates a [RequestBuilder] initialized with a `DELETE` method and parsed [uri] bytes. */
        fun delete(uri: ByteArray): RequestBuilder = methodWithUri(Method.DELETE, uri)

        /** Creates a [RequestBuilder] initialized with an `OPTIONS` method and [uri]. */
        fun options(uri: Uri): RequestBuilder = methodWithUri(Method.OPTIONS, uri)

        /** Creates a [RequestBuilder] initialized with an `OPTIONS` method and parsed [uri]. */
        fun options(uri: String): RequestBuilder = methodWithUri(Method.OPTIONS, uri)

        /** Creates a [RequestBuilder] initialized with an `OPTIONS` method and parsed [uri] bytes. */
        fun options(uri: ByteArray): RequestBuilder = methodWithUri(Method.OPTIONS, uri)

        /** Creates a [RequestBuilder] initialized with a `HEAD` method and [uri]. */
        fun head(uri: Uri): RequestBuilder = methodWithUri(Method.HEAD, uri)

        /** Creates a [RequestBuilder] initialized with a `HEAD` method and parsed [uri]. */
        fun head(uri: String): RequestBuilder = methodWithUri(Method.HEAD, uri)

        /** Creates a [RequestBuilder] initialized with a `HEAD` method and parsed [uri] bytes. */
        fun head(uri: ByteArray): RequestBuilder = methodWithUri(Method.HEAD, uri)

        /** Creates a [RequestBuilder] initialized with a `CONNECT` method and [uri]. */
        fun connect(uri: Uri): RequestBuilder = methodWithUri(Method.CONNECT, uri)

        /** Creates a [RequestBuilder] initialized with a `CONNECT` method and parsed [uri]. */
        fun connect(uri: String): RequestBuilder = methodWithUri(Method.CONNECT, uri)

        /** Creates a [RequestBuilder] initialized with a `CONNECT` method and parsed [uri] bytes. */
        fun connect(uri: ByteArray): RequestBuilder = methodWithUri(Method.CONNECT, uri)

        /** Creates a [RequestBuilder] initialized with a `PATCH` method and [uri]. */
        fun patch(uri: Uri): RequestBuilder = methodWithUri(Method.PATCH, uri)

        /** Creates a [RequestBuilder] initialized with a `PATCH` method and parsed [uri]. */
        fun patch(uri: String): RequestBuilder = methodWithUri(Method.PATCH, uri)

        /** Creates a [RequestBuilder] initialized with a `PATCH` method and parsed [uri] bytes. */
        fun patch(uri: ByteArray): RequestBuilder = methodWithUri(Method.PATCH, uri)

        /** Creates a [RequestBuilder] initialized with a `TRACE` method and [uri]. */
        fun trace(uri: Uri): RequestBuilder = methodWithUri(Method.TRACE, uri)

        /** Creates a [RequestBuilder] initialized with a `TRACE` method and parsed [uri]. */
        fun trace(uri: String): RequestBuilder = methodWithUri(Method.TRACE, uri)

        /** Creates a [RequestBuilder] initialized with a `TRACE` method and parsed [uri] bytes. */
        fun trace(uri: ByteArray): RequestBuilder = methodWithUri(Method.TRACE, uri)

        /** Creates a new blank [Request] with the given [body]. */
        fun <T : Any> new(body: T): Request<T> = Request(head = RequestParts.new(), bodyValue = body)

        /** Creates a new blank [Request] with an empty [Unit] body (Swift-friendly). */
        fun newEmpty(): Request<Unit> = new(Unit)

        /** Creates a new blank [Request] with a UTF-8 [String] body (Swift-friendly). */
        fun newText(body: String): Request<String> = new(body)

        /** Creates a new blank [Request] with a raw [ByteArray] body (Swift-friendly). */
        fun newBytes(body: ByteArray): Request<ByteArray> = new(body)

        /** Creates a new [Request] with the given component [parts] and [body]. */
        fun <T : Any> fromParts(
            parts: RequestParts,
            body: T,
        ): Request<T> = Request(head = parts, bodyValue = body)

        /** Creates a new [Request] with the given [parts] and [String] body (Swift-friendly). */
        fun fromPartsText(
            parts: RequestParts,
            body: String,
        ): Request<String> = fromParts(parts, body)

        private fun methodWithUri(
            method: Method,
            uri: Uri,
        ): RequestBuilder = RequestBuilder.new().method(method).uri(uri)

        private fun methodWithUri(
            method: Method,
            uri: String,
        ): RequestBuilder = RequestBuilder.new().method(method).uri(uri)

        private fun methodWithUri(
            method: Method,
            uri: ByteArray,
        ): RequestBuilder = RequestBuilder.new().method(method).uri(uri)
    }
}

/**
 * Component parts of an HTTP [Request].
 *
 * The HTTP request head consists of a method, URI, version, a set of header fields,
 * and protocol extensions.
 */
class RequestParts internal constructor(
    /** The request's method. */
    var method: Method,
    /** The request's URI. */
    var uri: Uri,
    /** The request's version. */
    var version: Version,
    /** The request's headers. */
    val headers: HeaderMap<HeaderValue>,
    /** The request's extensions. */
    var extensions: Extensions,
) {
    /** Clones all head fields into an independent [RequestParts] instance. */
    fun clone(): RequestParts =
        RequestParts(
            method = method.clone(),
            uri = uri.clone(),
            version = version.clone(),
            headers = headers.clone(),
            extensions = extensions.clone(),
        )

    /** Formats a debug representation matching `Parts { ... }`. */
    fun debugString(): String =
        "Parts { method: $method, uri: $uri, version: $version, headers: ${headers.debugString()} }"

    override fun toString(): String = debugString()

    companion object {
        internal fun new(): RequestParts =
            RequestParts(
                method = Method.default(),
                uri = Uri.default(),
                version = Version.default(),
                headers = HeaderMap.default(),
                extensions = Extensions.default(),
            )
    }
}

/**
 * An HTTP request builder.
 *
 * This type constructs an instance of [Request] through a builder-style pattern,
 * accumulating parse or header-capacity errors until [body] is called.
 */
class RequestBuilder private constructor(
    private val inner: HttpResult<RequestParts>,
) {
    /** Creates a new default instance of [RequestBuilder]. */
    constructor() : this(HttpResult.ok(RequestParts.new()))

    /** Sets the HTTP method for this request. Defaults to `GET`. */
    fun method(method: Method): RequestBuilder =
        andThen { head ->
            head.method = method
            HttpResult.ok(head)
        }

    /** Parses and sets the HTTP method from a string. */
    fun method(method: String): RequestBuilder = parseMethod(Method.tryFrom(method))

    /** Parses and sets the HTTP method from raw bytes. */
    fun method(method: ByteArray): RequestBuilder = parseMethod(Method.tryFrom(method))

    /** Returns the HTTP method configured on this builder, or null if the builder has failed. */
    fun methodRef(): Method? = inner.value?.method

    /** Sets the URI for this request. Defaults to `/`. */
    fun uri(uri: Uri): RequestBuilder =
        andThen { head ->
            head.uri = uri
            HttpResult.ok(head)
        }

    /** Parses and sets the URI from a string. */
    fun uri(uri: String): RequestBuilder = parseUri(Uri.tryFrom(uri))

    /** Parses and sets the URI from raw bytes. */
    fun uri(uri: ByteArray): RequestBuilder = parseUri(Uri.tryFrom(uri))

    /** Returns the URI configured on this builder, or null if the builder has failed. */
    fun uriRef(): Uri? = inner.value?.uri

    /** Sets the HTTP version for this request. Defaults to `HTTP/1.1`. */
    fun version(version: Version): RequestBuilder =
        andThen { head ->
            head.version = version
            HttpResult.ok(head)
        }

    /** Returns the HTTP version configured on this builder, or null if the builder has failed. */
    fun versionRef(): Version? = inner.value?.version

    /** Appends a header with [HeaderName] key and [HeaderValue] value. */
    fun header(
        key: HeaderName,
        value: HeaderValue,
    ): RequestBuilder = appendWithName(key) { Result.success(value) }

    /** Appends a header with [HeaderName] key and parsed string [value]. */
    fun header(
        key: HeaderName,
        value: String,
    ): RequestBuilder = appendWithName(key) { HeaderValue.fromStr(value) }

    /** Appends a header with [HeaderName] key and parsed [ByteArray] [value]. */
    fun header(
        key: HeaderName,
        value: ByteArray,
    ): RequestBuilder = appendWithName(key) { HeaderValue.fromBytes(value) }

    /** Appends a header with [HeaderName] key and decimal integer [value]. */
    fun header(
        key: HeaderName,
        value: Int,
    ): RequestBuilder = appendWithName(key) { HeaderValue.fromStr(value.toString()) }

    /** Appends a header with [HeaderName] key and decimal long [value]. */
    fun header(
        key: HeaderName,
        value: Long,
    ): RequestBuilder = appendWithName(key) { HeaderValue.fromStr(value.toString()) }

    /** Appends a header with parsed string [key] and [HeaderValue] [value]. */
    fun header(
        key: String,
        value: HeaderValue,
    ): RequestBuilder = appendWithKeyString(key) { Result.success(value) }

    /** Appends a header with parsed string [key] and string [value]. */
    fun header(
        key: String,
        value: String,
    ): RequestBuilder = appendWithKeyString(key) { HeaderValue.fromStr(value) }

    /** Appends a header with parsed string [key] and [ByteArray] [value]. */
    fun header(
        key: String,
        value: ByteArray,
    ): RequestBuilder = appendWithKeyString(key) { HeaderValue.fromBytes(value) }

    /** Appends a header with parsed string [key] and decimal integer [value]. */
    fun header(
        key: String,
        value: Int,
    ): RequestBuilder = appendWithKeyString(key) { HeaderValue.fromStr(value.toString()) }

    /** Appends a header with parsed string [key] and decimal long [value]. */
    fun header(
        key: String,
        value: Long,
    ): RequestBuilder = appendWithKeyString(key) { HeaderValue.fromStr(value.toString()) }

    /** Returns the configured headers, or null if the builder has failed. */
    fun headersRef(): HeaderMap<HeaderValue>? = inner.value?.headers

    /** Returns mutable access to the configured headers, or null if the builder has failed. */
    fun headersMut(): HeaderMap<HeaderValue>? = inner.value?.headers

    /** Adds an extension value to this builder. */
    inline fun <reified T : Any> extension(extension: T): RequestBuilder =
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
    ): RequestBuilder =
        andThen { head ->
            head.extensions.insert(type, typeName, value, clone)
            HttpResult.ok(head)
        }

    /** Returns the extensions map configured on this builder, or null if the builder has failed. */
    fun extensionsRef(): Extensions? = inner.value?.extensions

    /** Returns mutable access to the extensions map, or null if the builder has failed. */
    fun extensionsMut(): Extensions? = inner.value?.extensions

    /** Consumes this builder and builds a [Request] with the given [body], or returns the accumulated [Error]. */
    fun <T : Any> body(body: T): HttpResult<Request<T>> = inner.map { head -> Request(head = head, bodyValue = body) }

    /** Consumes this builder and builds a [Request] with a [Unit] body (Swift-friendly). */
    fun bodyEmpty(): HttpResult<Request<Unit>> = body(Unit)

    /** Consumes this builder and builds a [Request] with a [String] body (Swift-friendly). */
    fun bodyText(body: String): HttpResult<Request<String>> = body(body)

    /** Consumes this builder and builds a [Request] with a [ByteArray] body (Swift-friendly). */
    fun bodyBytes(body: ByteArray): HttpResult<Request<ByteArray>> = body(body)

    /** Formats a debug representation matching `Builder { inner: ... }`. */
    fun debugString(): String {
        val state = inner.value?.debugString() ?: inner.error?.debugString()
        val label = if (inner.isOk()) "Ok" else "Err"
        return "Builder { inner: $label($state) }"
    }

    override fun toString(): String = debugString()

    private fun parseMethod(result: Result<Method>): RequestBuilder =
        andThen { head ->
            result.fold(
                onSuccess = { parsed ->
                    head.method = parsed
                    HttpResult.ok(head)
                },
                onFailure = { err -> HttpResult.err(errorFromThrowable(err)) },
            )
        }

    private fun parseUri(result: Result<Uri>): RequestBuilder =
        andThen { head ->
            result.fold(
                onSuccess = { parsed ->
                    head.uri = parsed
                    HttpResult.ok(head)
                },
                onFailure = { err -> HttpResult.err(errorFromThrowable(err)) },
            )
        }

    private inline fun appendWithName(
        key: HeaderName,
        crossinline makeValue: () -> Result<HeaderValue>,
    ): RequestBuilder =
        andThen { head ->
            val err = appendHeaderValue(head.headers, key, makeValue)
            if (err == null) HttpResult.ok(head) else HttpResult.err(err)
        }

    private inline fun appendWithKeyString(
        key: String,
        crossinline makeValue: () -> Result<HeaderValue>,
    ): RequestBuilder =
        andThen { head ->
            val err = appendHeaderNameAndValue(head.headers, key, makeValue)
            if (err == null) HttpResult.ok(head) else HttpResult.err(err)
        }

    private fun andThen(func: (RequestParts) -> HttpResult<RequestParts>): RequestBuilder =
        RequestBuilder(inner = inner.andThen(func))

    companion object {
        /** Creates a new default [RequestBuilder]. */
        fun new(): RequestBuilder = RequestBuilder()

        /** Alias for [new] matching Rust `Default::default`. */
        fun default(): RequestBuilder = RequestBuilder()
    }
}

internal inline fun appendHeaderValue(
    headers: HeaderMap<HeaderValue>,
    key: HeaderName,
    makeValue: () -> Result<HeaderValue>,
): Error? {
    val value = makeValue().getOrElse { return errorFromThrowable(it) }
    headers.tryAppend(key, value).getOrElse { return errorFromThrowable(it) }
    return null
}

internal inline fun appendHeaderNameAndValue(
    headers: HeaderMap<HeaderValue>,
    key: String,
    makeValue: () -> Result<HeaderValue>,
): Error? {
    val parsedKey = HeaderName.fromStr(key)
    val name = parsedKey.value ?: return Error.from(checkNotNull(parsedKey.error))
    return appendHeaderValue(headers, name, makeValue)
}
