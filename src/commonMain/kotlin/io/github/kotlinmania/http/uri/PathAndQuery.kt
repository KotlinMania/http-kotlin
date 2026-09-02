// port-lint: source uri/path.rs
package io.github.kotlinmania.http.uri

import io.github.kotlinmania.bytes.Bytes
import io.github.kotlinmania.http.ByteStr

/**
 * Represents the path and query component of a URI.
 */
class PathAndQuery internal constructor(
    internal val data: ByteStr,
    internal val query: Int,
) : Comparable<PathAndQuery> {
    /**
     * Returns the path component.
     */
    fun path(): String {
        val str = data.asStr()
        val ret = if (query == NONE) {
            str
        } else {
            str.substring(0, query)
        }
        return if (ret.isEmpty()) "/" else ret
    }

    /**
     * Returns the query string component.
     */
    fun query(): String? {
        if (query == NONE) return null
        val str = data.asStr()
        val i = query + 1
        return if (i <= str.length) str.substring(i) else ""
    }

    /**
     * Returns the path and query as a string component.
     */
    fun asStr(): String {
        val ret = data.asStr()
        return if (ret.isEmpty()) "/" else ret
    }

    fun asRef(): String = asStr()

    override fun toString(): String {
        val str = data.asStr()
        return if (str.isNotEmpty()) {
            if (str.startsWith('/') || str.startsWith('*')) {
                str
            } else {
                "/$str"
            }
        } else {
            "/"
        }
    }

    fun eq(other: String): Boolean = asStr() == other

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is PathAndQuery) {
            return data == other.data
        }
        if (other is String) {
            return asStr() == other
        }
        return false
    }

    override fun compareTo(other: PathAndQuery): Int =
        asStr().compareTo(other.asStr())

    fun compareTo(other: String): Int =
        asStr().compareTo(other)

    override fun hashCode(): Int = data.hashCode()

    companion object {
        internal const val NONE: Int = 0xFFFF

        internal fun empty(): PathAndQuery = PathAndQuery(ByteStr.new(), NONE)

        internal fun slash(): PathAndQuery = PathAndQuery(ByteStr.fromStatic("/"), NONE)

        internal fun star(): PathAndQuery = PathAndQuery(ByteStr.fromStatic("*"), NONE)

        fun fromStatic(src: String): PathAndQuery {
            val queryPos = validatePathAndQueryBytes(src.encodeToByteArray())
            if (queryPos < 0) {
                throw IllegalArgumentException("static str is not valid path")
            }
            return PathAndQuery(ByteStr.fromStatic(src), queryPos)
        }

        internal fun fromShared(src: Bytes): Result<PathAndQuery> {
            var query = NONE
            var fragment: Int? = null
            var isMaybeNotUtf8 = false

            val bytes = src.asRef()
            var i = 0

            // path
            while (i < bytes.size) {
                val b = bytes[i].toInt() and 0xFF
                if (b == '?'.code) {
                    query = i
                    i++
                    break
                }
                if (b == '#'.code) {
                    fragment = i
                    break
                }

                val allowed = b == 0x21
                    || (b in 0x24..0x3B)
                    || b == 0x3D
                    || (b in 0x40..0x5F)
                    || (b in 0x61..0x7A)
                    || b == 0x7C
                    || b == 0x7E
                    || b == '"'.code
                    || b == '{'.code
                    || b == '}'.code

                if (allowed) {
                    // ok
                } else if (b in 0x7F..0xFF) {
                    isMaybeNotUtf8 = true
                } else {
                    return Result.failure(InvalidUri.of(ErrorKind.InvalidUriChar))
                }
                i++
            }

            // query
            if (query != NONE && fragment == null) {
                while (i < bytes.size) {
                    val b = bytes[i].toInt() and 0xFF
                    if (b == '#'.code) {
                        fragment = i
                        break
                    }

                    val allowed = b == 0x21
                        || (b in 0x24..0x3B)
                        || b == 0x3D
                        || (b in 0x3F..0x7E)

                    if (allowed) {
                        // ok
                    } else if (b in 0x7F..0xFF) {
                        isMaybeNotUtf8 = true
                    } else {
                        return Result.failure(InvalidUri.of(ErrorKind.InvalidUriChar))
                    }
                    i++
                }
            }

            val finalBytes = if (fragment != null) {
                src.slice(0, fragment)
            } else {
                src
            }

            val data = if (isMaybeNotUtf8) {
                ByteStr.fromUtf8(finalBytes).getOrElse {
                    return Result.failure(InvalidUri.of(ErrorKind.InvalidUriChar))
                }
            } else {
                ByteStr.fromUtf8Unchecked(finalBytes)
            }

            return Result.success(PathAndQuery(data, query))
        }

        fun tryFrom(bytes: ByteArray): Result<PathAndQuery> =
            fromShared(Bytes.copyFromSlice(bytes))

        fun tryFrom(s: String): Result<PathAndQuery> = tryFrom(s.encodeToByteArray())

        fun fromStr(s: String): Result<PathAndQuery> = tryFrom(s)
    }
}

internal fun validatePathAndQueryBytes(bytes: ByteArray): Int {
    var query = PathAndQuery.NONE
    var i = 0

    while (i < bytes.size) {
        val b = bytes[i].toInt() and 0xFF
        if (b == '?'.code) {
            query = i
            i++
            break
        } else if (b == '#'.code) {
            return -1
        } else {
            val allowed = b == 0x21
                || (b in 0x24..0x3B)
                || b == 0x3D
                || (b in 0x40..0x5F)
                || (b in 0x61..0x7A)
                || b == 0x7C
                || b == 0x7E
                || b == '"'.code
                || b == '{'.code
                || b == '}'.code
                || (b >= 0x7F)

            if (!allowed) {
                return -1
            }
        }
        i++
    }

    if (query != PathAndQuery.NONE) {
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            if (b == '#'.code) {
                return -1
            }

            val allowed = b == 0x21
                || (b in 0x24..0x3B)
                || b == 0x3D
                || (b in 0x3F..0x7E)
                || (b >= 0x7F)

            if (!allowed) {
                return -1
            }

            i++
        }
    }

    return query
}
