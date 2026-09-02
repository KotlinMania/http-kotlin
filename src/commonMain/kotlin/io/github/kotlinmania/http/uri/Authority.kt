// port-lint: source uri/authority.rs
package io.github.kotlinmania.http.uri

import io.github.kotlinmania.bytes.Bytes
import io.github.kotlinmania.http.ByteStr

/**
 * Represents the authority component of a URI.
 */
class Authority internal constructor(
    internal val data: ByteStr,
) : Comparable<Authority> {
    /**
     * Get the host of this `Authority`.
     */
    fun host(): String = host(asStr())

    /**
     * Get the port part of this `Authority`.
     */
    fun port(): Port? {
        val bytes = asStr()
        val i = bytes.lastIndexOf(':')
        if (i == -1) return null
        return Port.fromStr(bytes.substring(i + 1)).getOrNull()
    }

    /**
     * Get the port of this `Authority` as an Int (u16).
     */
    fun portU16(): Int? = port()?.asU16()

    /**
     * Return a str representation of the authority.
     */
    fun asStr(): String = data.asStr()

    fun asRef(): String = asStr()

    override fun toString(): String = asStr()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is Authority) {
            return asStr().equals(other.asStr(), ignoreCase = true)
        }
        if (other is String) {
            return asStr().equals(other, ignoreCase = true)
        }
        return false
    }

    /**
     * Case-insensitive equality with a string.
     */
    fun eq(other: String): Boolean = asStr().equals(other, ignoreCase = true)

    override fun compareTo(other: Authority): Int =
        asStr().compareTo(other.asStr(), ignoreCase = true)

    fun compareTo(other: String): Int =
        asStr().compareTo(other, ignoreCase = true)

    override fun hashCode(): Int {
        val s = asStr()
        var h = s.length
        for (ch in s) {
            h = 31 * h + ch.lowercaseChar().code
        }
        return h
    }

    companion object {
        internal fun empty(): Authority = Authority(ByteStr.new())

        fun fromStatic(src: String): Authority {
            val res = validateAuthorityBytes(src.encodeToByteArray())
            if (res.isFailure) {
                throw IllegalArgumentException("static str is not valid authority")
            }
            return Authority(ByteStr.fromStatic(src))
        }

        internal fun fromShared(bytes: Bytes): Result<Authority> {
            val s = bytes.asRef()
            val authEnd = parseNonEmpty(s).getOrElse { return Result.failure(it) }
            if (authEnd != s.size) {
                return Result.failure(InvalidUri.of(ErrorKind.InvalidUriChar))
            }
            return Result.success(Authority(ByteStr.fromUtf8Unchecked(bytes)))
        }

        internal fun parse(s: ByteArray): Result<Int> =
            validateAuthorityBytes(s)

        internal fun parseNonEmpty(s: ByteArray): Result<Int> {
            if (s.isEmpty()) {
                return Result.failure(InvalidUri.of(ErrorKind.Empty))
            }
            return parse(s)
        }

        fun tryFrom(bytes: ByteArray): Result<Authority> {
            val authEnd = parseNonEmpty(bytes).getOrElse { return Result.failure(it) }
            if (authEnd != bytes.size) {
                return Result.failure(InvalidUri.of(ErrorKind.InvalidUriChar))
            }
            val b = Bytes.copyFromSlice(bytes)
            return Result.success(Authority(ByteStr.fromUtf8Unchecked(b)))
        }

        fun tryFrom(s: String): Result<Authority> = tryFrom(s.encodeToByteArray())

        fun fromStr(s: String): Result<Authority> = tryFrom(s)
    }
}

internal fun host(auth: String): String {
    val hostPort = auth.substringAfterLast('@')
    return if (hostPort.isNotEmpty() && hostPort.startsWith('[')) {
        val idx = hostPort.indexOf(']')
        if (idx != -1) hostPort.substring(0, idx + 1) else hostPort
    } else {
        hostPort.substringBefore(':')
    }
}

internal fun validateAuthorityBytes(s: ByteArray): Result<Int> {
    if (s.isEmpty()) {
        return Result.failure(InvalidUri.of(ErrorKind.Empty))
    }

    var colonCnt = 0
    var startBracket = false
    var endBracket = false
    var hasPercent = false
    var end = s.size
    var atSignPos = s.size
    val maxColons = 8

    var i = 0
    while (i < s.size) {
        val b = s[i]
        val idx = b.toInt() and 0xFF
        val ch = URI_CHARS[idx].toInt()

        if (ch == '/'.code || ch == '?'.code || ch == '#'.code) {
            end = i
            break
        }

        if (ch == 0) {
            if (b == '%'.code.toByte()) {
                hasPercent = true
            } else {
                return Result.failure(InvalidUri.of(ErrorKind.InvalidUriChar))
            }
        } else if (ch == ':'.code) {
            if (colonCnt >= maxColons) {
                return Result.failure(InvalidUri.of(ErrorKind.InvalidAuthority))
            }
            colonCnt++
        } else if (ch == '['.code) {
            if (hasPercent || startBracket) {
                return Result.failure(InvalidUri.of(ErrorKind.InvalidAuthority))
            }
            startBracket = true
        } else if (ch == ']'.code) {
            if (!startBracket || endBracket) {
                return Result.failure(InvalidUri.of(ErrorKind.InvalidAuthority))
            }
            endBracket = true
            colonCnt = 0
            hasPercent = false
        } else if (ch == '@'.code) {
            atSignPos = i
            colonCnt = 0
            hasPercent = false
        }
        i++
    }

    if (startBracket != endBracket) {
        return Result.failure(InvalidUri.of(ErrorKind.InvalidAuthority))
    }

    if (colonCnt > 1) {
        return Result.failure(InvalidUri.of(ErrorKind.InvalidAuthority))
    }

    if (end > 0 && atSignPos == end - 1) {
        return Result.failure(InvalidUri.of(ErrorKind.InvalidAuthority))
    }

    if (hasPercent) {
        return Result.failure(InvalidUri.of(ErrorKind.InvalidAuthority))
    }

    return Result.success(end)
}

internal val URI_CHARS: ByteArray = ByteArray(256).apply {
    this['!'.code] = '!'.code.toByte()
    this['#'.code] = '#'.code.toByte()
    this['$'.code] = '$'.code.toByte()
    this['&'.code] = '&'.code.toByte()
    this['\''.code] = '\''.code.toByte()
    this['('.code] = '('.code.toByte()
    this[')'.code] = ')'.code.toByte()
    this['*'.code] = '*'.code.toByte()
    this['+'.code] = '+'.code.toByte()
    this[','.code] = ','.code.toByte()
    this['-'.code] = '-'.code.toByte()
    this['.'.code] = '.'.code.toByte()
    this['/'.code] = '/'.code.toByte()
    for (c in '0'..'9') this[c.code] = c.code.toByte()
    this[':'.code] = ':'.code.toByte()
    this[';'.code] = ';'.code.toByte()
    this['='.code] = '='.code.toByte()
    this['?'.code] = '?'.code.toByte()
    this['@'.code] = '@'.code.toByte()
    for (c in 'A'..'Z') this[c.code] = c.code.toByte()
    this['['.code] = '['.code.toByte()
    this[']'.code] = ']'.code.toByte()
    this['_'.code] = '_'.code.toByte()
    for (c in 'a'..'z') this[c.code] = c.code.toByte()
    this['~'.code] = '~'.code.toByte()
}
