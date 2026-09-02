// port-lint: source uri/scheme.rs
package io.github.kotlinmania.http.uri

import io.github.kotlinmania.bytes.Bytes
import io.github.kotlinmania.http.ByteStr

/**
 * Represents the scheme component of a URI.
 */
class Scheme internal constructor(
    internal val inner: Scheme2,
) {
    /**
     * Return a string representation of the scheme.
     */
    fun asStr(): String =
        when (val s = inner) {
            is Scheme2.Standard ->
                when (s.protocol) {
                    Protocol.Http -> "http"
                    Protocol.Https -> "https"
                }
            is Scheme2.Other -> s.str.asStr()
            Scheme2.None -> error("Scheme::None has no string representation")
        }

    fun asRef(): String = asStr()

    override fun toString(): String = asStr()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Scheme) return false

        return when {
            inner is Scheme2.Standard && other.inner is Scheme2.Standard ->
                inner.protocol == other.inner.protocol
            inner is Scheme2.Other && other.inner is Scheme2.Other ->
                inner.str.asStr().equals(other.inner.str.asStr(), ignoreCase = true)
            inner is Scheme2.None && other.inner is Scheme2.None -> true
            else -> false
        }
    }

    /**
     * Case-insensitive equality with a string.
     */
    fun eq(other: String): Boolean = asStr().equals(other, ignoreCase = true)

    override fun hashCode(): Int =
        when (val s = inner) {
            Scheme2.None -> 0
            is Scheme2.Standard ->
                when (s.protocol) {
                    Protocol.Http -> 1
                    Protocol.Https -> 2
                }
            is Scheme2.Other -> {
                val str = s.str.asStr()
                var h = str.length
                for (ch in str) {
                    h = 31 * h + ch.lowercaseChar().code
                }
                h
            }
        }

    companion object {
        /** HTTP protocol scheme. */
        val HTTP: Scheme = Scheme(Scheme2.Standard(Protocol.Http))

        /** HTTP protocol over TLS. */
        val HTTPS: Scheme = Scheme(Scheme2.Standard(Protocol.Https))

        internal fun empty(): Scheme = Scheme(Scheme2.None)

        fun tryFrom(bytes: ByteArray): Result<Scheme> =
            when (val parsed = Scheme2.parseExact(bytes)) {
                is Scheme2Result.Failure -> Result.failure(parsed.error)
                is Scheme2Result.Success -> {
                    when (val s = parsed.value) {
                        Scheme2.None -> Result.failure(InvalidUri.of(ErrorKind.InvalidScheme))
                        is Scheme2.Standard -> Result.success(Scheme(s))
                        is Scheme2.Other -> {
                            val b = Bytes.copyFromSlice(bytes)
                            val string = ByteStr.fromUtf8Unchecked(b)
                            Result.success(Scheme(Scheme2.Other(string)))
                        }
                    }
                }
            }

        fun tryFrom(s: String): Result<Scheme> = tryFrom(s.encodeToByteArray())

        fun fromStr(s: String): Result<Scheme> = tryFrom(s)
    }
}

internal sealed class Scheme2 {
    data object None : Scheme2()
    data class Standard(val protocol: Protocol) : Scheme2()
    data class Other(val str: ByteStr) : Scheme2()

    companion object {
        fun parseExact(s: ByteArray): Scheme2Result =
            when {
                s.contentEquals(HTTP_BYTES) -> Scheme2Result.Success(Standard(Protocol.Http))
                s.contentEquals(HTTPS_BYTES) -> Scheme2Result.Success(Standard(Protocol.Https))
                else -> {
                    if (s.size > MAX_SCHEME_LEN) {
                        Scheme2Result.Failure(InvalidUri.of(ErrorKind.SchemeTooLong))
                    } else {
                        for (b in s) {
                            val idx = b.toInt() and 0xFF
                            val ch = SCHEME_CHARS[idx].toInt()
                            if (ch == ':'.code || ch == 0) {
                                return Scheme2Result.Failure(InvalidUri.of(ErrorKind.InvalidScheme))
                            }
                        }
                        Scheme2Result.Success(Other(ByteStr.new()))
                    }
                }
            }

        fun parse(s: ByteArray): Result<SchemeParseResult> {
            if (s.size >= 7) {
                if (s.copyOfRange(0, 7).decodeToString().equals("http://", ignoreCase = true)) {
                    return Result.success(SchemeParseResult.Standard(Protocol.Http))
                }
            }

            if (s.size >= 8) {
                if (s.copyOfRange(0, 8).decodeToString().equals("https://", ignoreCase = true)) {
                    return Result.success(SchemeParseResult.Standard(Protocol.Https))
                }
            }

            if (s.size > 3) {
                for (i in s.indices) {
                    val b = s[i]
                    val idx = b.toInt() and 0xFF
                    val ch = SCHEME_CHARS[idx].toInt()

                    if (ch == ':'.code) {
                        if (s.size < i + 3) {
                            break
                        }
                        if (s[i + 1] != '/'.code.toByte() || s[i + 2] != '/'.code.toByte()) {
                            break
                        }
                        if (i > MAX_SCHEME_LEN) {
                            return Result.failure(InvalidUri.of(ErrorKind.SchemeTooLong))
                        }
                        return Result.success(SchemeParseResult.Other(i))
                    } else if (ch == 0) {
                        break
                    }
                }
            }

            return Result.success(SchemeParseResult.None)
        }
    }
}

internal sealed class Scheme2Result {
    data class Success(val value: Scheme2) : Scheme2Result()
    data class Failure(val error: InvalidUri) : Scheme2Result()
}

internal sealed class SchemeParseResult {
    data object None : SchemeParseResult()
    data class Standard(val protocol: Protocol) : SchemeParseResult()
    data class Other(val length: Int) : SchemeParseResult()
}

internal enum class Protocol {
    Http,
    Https,
    ;

    fun len(): Int =
        when (this) {
            Http -> 4
            Https -> 5
        }
}

private const val MAX_SCHEME_LEN: Int = 64
private val HTTP_BYTES: ByteArray = "http".encodeToByteArray()
private val HTTPS_BYTES: ByteArray = "https".encodeToByteArray()

private val SCHEME_CHARS: ByteArray = ByteArray(256).apply {
    this['+'.code] = '+'.code.toByte()
    this['-'.code] = '-'.code.toByte()
    this['.'.code] = '.'.code.toByte()
    for (c in '0'..'9') this[c.code] = c.code.toByte()
    this[':'.code] = ':'.code.toByte()
    for (c in 'A'..'Z') this[c.code] = c.code.toByte()
    for (c in 'a'..'z') this[c.code] = c.code.toByte()
    this['~'.code] = '~'.code.toByte()
}
