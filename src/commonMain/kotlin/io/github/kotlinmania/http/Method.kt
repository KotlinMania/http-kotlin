// port-lint: source method.rs
package io.github.kotlinmania.http

/**
 * The HTTP request method.
 *
 * This module contains HTTP-method related structs and errors and such. The
 * main type of this module, `Method`, is also reexported at the root of the
 * crate as `http::Method` and is intended for import through that location
 * primarily.
 *
 * # Examples
 *
 * ```
 * check(Method.GET == Method.fromBytes("GET".encodeToByteArray()).getOrThrow())
 * check(Method.GET.isIdempotent())
 * check(Method.POST.asStr() == "POST")
 * ```
 */

/**
 * The Request Method (VERB).
 *
 * This type also contains constants for a number of common HTTP methods such
 * as GET, POST, etc.
 *
 * Currently includes 8 variants representing the 8 methods defined in
 * [RFC 7230](https://tools.ietf.org/html/rfc7231#section-4.1), plus PATCH,
 * and an Extension variant for all extensions.
 */
class Method private constructor(
    private val inner: Inner,
) {
    companion object {
        /** GET */
        val GET: Method = Method(Inner.Get)

        /** POST */
        val POST: Method = Method(Inner.Post)

        /** PUT */
        val PUT: Method = Method(Inner.Put)

        /** DELETE */
        val DELETE: Method = Method(Inner.Delete)

        /** HEAD */
        val HEAD: Method = Method(Inner.Head)

        /** OPTIONS */
        val OPTIONS: Method = Method(Inner.Options)

        /** CONNECT */
        val CONNECT: Method = Method(Inner.Connect)

        /** PATCH */
        val PATCH: Method = Method(Inner.Patch)

        /** TRACE */
        val TRACE: Method = Method(Inner.Trace)

        /** Converts a slice of bytes to an HTTP method. */
        fun fromBytes(src: ByteArray): Result<Method> =
            when (src.size) {
                0 -> Result.failure(InvalidMethod())
                3 ->
                    when {
                        src.contentEquals(METHOD_GET) -> Result.success(Method(Inner.Get))
                        src.contentEquals(METHOD_PUT) -> Result.success(Method(Inner.Put))
                        else -> extensionInline(src)
                    }
                4 ->
                    when {
                        src.contentEquals(METHOD_POST) -> Result.success(Method(Inner.Post))
                        src.contentEquals(METHOD_HEAD) -> Result.success(Method(Inner.Head))
                        else -> extensionInline(src)
                    }
                5 ->
                    when {
                        src.contentEquals(METHOD_PATCH) -> Result.success(Method(Inner.Patch))
                        src.contentEquals(METHOD_TRACE) -> Result.success(Method(Inner.Trace))
                        else -> extensionInline(src)
                    }
                6 ->
                    when {
                        src.contentEquals(METHOD_DELETE) -> Result.success(Method(Inner.Delete))
                        else -> extensionInline(src)
                    }
                7 ->
                    when {
                        src.contentEquals(METHOD_OPTIONS) -> Result.success(Method(Inner.Options))
                        src.contentEquals(METHOD_CONNECT) -> Result.success(Method(Inner.Connect))
                        else -> extensionInline(src)
                    }
                else ->
                    if (src.size <= InlineExtension.MAX) {
                        extensionInline(src)
                    } else {
                        AllocatedExtension.new(src).map { allocated ->
                            Method(Inner.ExtensionAllocated(allocated))
                        }
                    }
            }

        fun tryFrom(src: ByteArray): Result<Method> {
            return fromBytes(src)
        }

        fun tryFrom(src: String): Result<Method> {
            return tryFrom(src.encodeToByteArray())
        }

        fun from(method: Method): Method {
            return method
        }

        fun fromStr(src: String): Result<Method> {
            return tryFrom(src)
        }

        fun default(): Method {
            return GET
        }

        private fun extensionInline(src: ByteArray): Result<Method> =
            InlineExtension.new(src).map { inline ->
                Method(Inner.ExtensionInline(inline))
            }
    }

    /**
     * Whether a method is considered "safe", meaning the request is
     * essentially read-only.
     *
     * See [the spec](https://tools.ietf.org/html/rfc7231#section-4.2.1)
     * for more words.
     */
    fun isSafe(): Boolean {
        return inner == Inner.Get ||
            inner == Inner.Head ||
            inner == Inner.Options ||
            inner == Inner.Trace
    }

    /**
     * Whether a method is considered "idempotent", meaning the request has
     * the same result if executed multiple times.
     *
     * See [the spec](https://tools.ietf.org/html/rfc7231#section-4.2.2) for
     * more words.
     */
    fun isIdempotent(): Boolean =
        when (inner) {
            Inner.Put,
            Inner.Delete,
            -> true
            else -> isSafe()
        }

    /** Return a String representation of the HTTP method. */
    fun asStr(): String =
        when (inner) {
            Inner.Options -> "OPTIONS"
            Inner.Get -> "GET"
            Inner.Post -> "POST"
            Inner.Put -> "PUT"
            Inner.Delete -> "DELETE"
            Inner.Head -> "HEAD"
            Inner.Trace -> "TRACE"
            Inner.Connect -> "CONNECT"
            Inner.Patch -> "PATCH"
            is Inner.ExtensionInline -> inner.inline.asStr()
            is Inner.ExtensionAllocated -> inner.allocated.asStr()
        }

    fun asRef(): String {
        return asStr()
    }

    fun eq(other: Method): Boolean {
        return this == other
    }

    fun eq(other: String): Boolean {
        return asRef() == other
    }

    fun fmt(): String {
        return asRef()
    }

    fun fmt(formatter: StringBuilder): StringBuilder {
        formatter.append(asRef())
        return formatter
    }

    override fun equals(other: Any?): Boolean =
        when (other) {
            is Method -> inner == other.inner
            else -> false
        }

    override fun hashCode(): Int = inner.hashCode()

    override fun toString(): String = asRef()
}

/** A possible error value when converting `Method` from bytes. */
class InvalidMethod internal constructor() : IllegalArgumentException("invalid HTTP method") {
    override fun toString(): String = "InvalidMethod"

    fun fmt(): String {
        return "InvalidMethod"
    }

    fun fmt(formatter: StringBuilder): StringBuilder {
        formatter.append("invalid HTTP method")
        return formatter
    }
}

private val METHOD_GET = "GET".encodeToByteArray()
private val METHOD_PUT = "PUT".encodeToByteArray()
private val METHOD_POST = "POST".encodeToByteArray()
private val METHOD_HEAD = "HEAD".encodeToByteArray()
private val METHOD_PATCH = "PATCH".encodeToByteArray()
private val METHOD_TRACE = "TRACE".encodeToByteArray()
private val METHOD_DELETE = "DELETE".encodeToByteArray()
private val METHOD_OPTIONS = "OPTIONS".encodeToByteArray()
private val METHOD_CONNECT = "CONNECT".encodeToByteArray()

private sealed class Inner {
    data object Options : Inner()
    data object Get : Inner()
    data object Post : Inner()
    data object Put : Inner()
    data object Delete : Inner()
    data object Head : Inner()
    data object Trace : Inner()
    data object Connect : Inner()
    data object Patch : Inner()

    /** If the extension is short enough, store it inline. */
    data class ExtensionInline(val inline: InlineExtension) : Inner()

    /** Otherwise, allocate it. */
    data class ExtensionAllocated(val allocated: AllocatedExtension) : Inner()
}

private data class InlineExtension(
    private val data: ByteArray,
    private val len: Int,
) {
    companion object {
        /** Method.fromBytes() assumes this is at least 7. */
        const val MAX: Int = 15

        fun new(src: ByteArray): Result<InlineExtension> {
            val data = ByteArray(MAX)

            val checked = writeChecked(src, data)
            if (checked.isFailure) {
                return Result.failure(checked.exceptionOrNull() ?: InvalidMethod())
            }

            // Invariant: writeChecked ensures that the first src.size bytes
            // of data are valid UTF-8.
            return Result.success(InlineExtension(data, src.size))
        }
    }

    fun asStr(): String {
        // Safety: the invariant of InlineExtension ensures that the first
        // len bytes of data contain valid UTF-8.
        return data.copyOfRange(0, len).decodeToString()
    }

    override fun equals(other: Any?): Boolean =
        other is InlineExtension &&
            len == other.len &&
            data.copyOfRange(0, len).contentEquals(other.data.copyOfRange(0, other.len))

    override fun hashCode(): Int = data.copyOfRange(0, len).contentHashCode()
}

private data class AllocatedExtension(
    private val data: ByteArray,
) {
    companion object {
        fun new(src: ByteArray): Result<AllocatedExtension> {
            val data = ByteArray(src.size)

            val checked = writeChecked(src, data)
            if (checked.isFailure) {
                return Result.failure(checked.exceptionOrNull() ?: InvalidMethod())
            }

            // Invariant: data is exactly src.size long and writeChecked
            // ensures that the first src.size bytes of data are valid UTF-8.
            return Result.success(AllocatedExtension(data))
        }
    }

    fun asStr(): String {
        // Safety: the invariant of AllocatedExtension ensures that data
        // contains valid UTF-8.
        return data.decodeToString()
    }

    override fun equals(other: Any?): Boolean =
        other is AllocatedExtension && data.contentEquals(other.data)

    override fun hashCode(): Int = data.contentHashCode()
}

// From the RFC 9110 HTTP Semantics, section 9.1, the HTTP method is case-sensitive and can
// contain the following characters:
//
// ```
// method = token
// token = 1*tchar
// tchar = "!" / "#" / "$" / "%" / "&" / "'" / "*" / "+" / "-" / "." /
//     "^" / "_" / "`" / "|" / "~" / DIGIT / ALPHA
// ```
//
// https://datatracker.ietf.org/doc/html/rfc9110#section-9.1
//
// Note that this definition means that any ByteArray that consists solely of valid
// characters is also valid UTF-8 because the valid method characters are a
// subset of the valid 1 byte UTF-8 encoding.
private val METHOD_CHARS: ByteArray =
    byteArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, '!'.code.toByte(), 0, '#'.code.toByte(), '$'.code.toByte(), '%'.code.toByte(), '&'.code.toByte(), '\''.code.toByte(),
        0, 0, '*'.code.toByte(), '+'.code.toByte(), 0, '-'.code.toByte(), '.'.code.toByte(), 0, '0'.code.toByte(), '1'.code.toByte(),
        '2'.code.toByte(), '3'.code.toByte(), '4'.code.toByte(), '5'.code.toByte(), '6'.code.toByte(), '7'.code.toByte(), '8'.code.toByte(), '9'.code.toByte(), 0, 0,
        0, 0, 0, 0, 0, 'A'.code.toByte(), 'B'.code.toByte(), 'C'.code.toByte(), 'D'.code.toByte(), 'E'.code.toByte(),
        'F'.code.toByte(), 'G'.code.toByte(), 'H'.code.toByte(), 'I'.code.toByte(), 'J'.code.toByte(), 'K'.code.toByte(), 'L'.code.toByte(), 'M'.code.toByte(), 'N'.code.toByte(), 'O'.code.toByte(),
        'P'.code.toByte(), 'Q'.code.toByte(), 'R'.code.toByte(), 'S'.code.toByte(), 'T'.code.toByte(), 'U'.code.toByte(), 'V'.code.toByte(), 'W'.code.toByte(), 'X'.code.toByte(), 'Y'.code.toByte(),
        'Z'.code.toByte(), 0, 0, 0, '^'.code.toByte(), '_'.code.toByte(), '`'.code.toByte(), 'a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte(),
        'd'.code.toByte(), 'e'.code.toByte(), 'f'.code.toByte(), 'g'.code.toByte(), 'h'.code.toByte(), 'i'.code.toByte(), 'j'.code.toByte(), 'k'.code.toByte(), 'l'.code.toByte(), 'm'.code.toByte(),
        'n'.code.toByte(), 'o'.code.toByte(), 'p'.code.toByte(), 'q'.code.toByte(), 'r'.code.toByte(), 's'.code.toByte(), 't'.code.toByte(), 'u'.code.toByte(), 'v'.code.toByte(), 'w'.code.toByte(),
        'x'.code.toByte(), 'y'.code.toByte(), 'z'.code.toByte(), 0, '|'.code.toByte(), 0, '~'.code.toByte(), 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0,
    )

// writeChecked ensures, among other things, that the first src.size bytes
// of dst are valid UTF-8.
private fun writeChecked(
    src: ByteArray,
    dst: ByteArray,
): Result<Unit> {
    for (i in src.indices) {
        val b = METHOD_CHARS[src[i].toInt() and 0xff]

        if (b == 0.toByte()) {
            return Result.failure(InvalidMethod())
        }

        dst[i] = b
    }

    return Result.success(Unit)
}
