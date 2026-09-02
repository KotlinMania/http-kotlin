// port-lint: source uri/mod.rs
package io.github.kotlinmania.http.uri

import io.github.kotlinmania.bytes.Bytes
import io.github.kotlinmania.http.ByteStr

/**
 * The URI component of a request.
 */
class Uri internal constructor(
    private val scheme: Scheme,
    private val authority: Authority,
    private val pathAndQuery: PathAndQuery,
) {
    /**
     * Get the path of this [Uri].
     */
    fun path(): String =
        if (hasPath()) {
            pathAndQuery.path()
        } else {
            ""
        }

    /**
     * Get the scheme of this [Uri].
     */
    fun scheme(): Scheme? =
        if (scheme.inner is Scheme2.None) {
            null
        } else {
            scheme
        }

    /**
     * Get the scheme of this [Uri] as a string.
     */
    fun schemeStr(): String? = scheme()?.asStr()

    /**
     * Get the authority of this [Uri].
     */
    fun authority(): Authority? =
        if (authority.data.asStr().isEmpty()) {
            null
        } else {
            authority
        }

    /**
     * Get the host of this [Uri].
     */
    fun host(): String? = authority()?.host()

    /**
     * Get the port part of this [Uri].
     */
    fun port(): Port? = authority()?.port()

    /**
     * Get the port of this [Uri] as an Int (u16).
     */
    fun portU16(): Int? = port()?.asU16()

    /**
     * Get the query string of this [Uri], starting after the `?`.
     */
    fun query(): String? = pathAndQuery.query()

    /**
     * Returns the path & query components of the Uri.
     */
    fun pathAndQuery(): PathAndQuery? =
        if (scheme.inner !is Scheme2.None || authority.data.asStr().isEmpty()) {
            pathAndQuery
        } else {
            null
        }

    /**
     * Convert a [Uri] into [Parts].
     */
    fun intoParts(): Parts {
        val pq = if (hasPath()) pathAndQuery else null
        val sch = scheme()
        val auth = authority()
        return Parts(
            scheme = sch,
            authority = auth,
            pathAndQuery = pq,
        )
    }

    private fun hasPath(): Boolean =
        pathAndQuery.data.asStr().isNotEmpty() || scheme.inner !is Scheme2.None

    override fun toString(): String = buildString {
        val s = scheme()
        if (s != null) {
            append(s.asStr())
            append("://")
        }
        val a = authority()
        if (a != null) {
            append(a.asStr())
        }
        append(path())
        val q = query()
        if (q != null) {
            append('?')
            append(q)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is Uri) {
            return scheme() == other.scheme() &&
                authority() == other.authority() &&
                path() == other.path() &&
                query() == other.query()
        }
        if (other is String) {
            return eqStr(other)
        }
        return false
    }

    fun eq(other: String): Boolean = eqStr(other)

    private fun eqStr(rawStr: String): Boolean {
        var other = rawStr
        var absolute = false

        val s = scheme()
        if (s != null) {
            val schemeStr = s.asStr()
            absolute = true
            if (other.length < schemeStr.length + 3) return false
            if (!other.substring(0, schemeStr.length).equals(schemeStr, ignoreCase = true)) return false
            other = other.substring(schemeStr.length)
            if (!other.startsWith("://")) return false
            other = other.substring(3)
        }

        val auth = authority()
        if (auth != null) {
            val authStr = auth.asStr()
            absolute = true
            if (other.length < authStr.length) return false
            if (!other.substring(0, authStr.length).equals(authStr, ignoreCase = true)) return false
            other = other.substring(authStr.length)
        }

        val p = path()
        if (other.length < p.length || !other.startsWith(p)) {
            if (absolute && p == "/") {
                // PathAndQuery can be omitted
            } else {
                return false
            }
        } else {
            other = other.substring(p.length)
        }

        val q = query()
        if (q != null) {
            if (other.isEmpty()) return q.isEmpty()
            if (!other.startsWith('?')) return false
            other = other.substring(1)
            if (other.length < q.length) return false
            if (!other.startsWith(q)) return false
            other = other.substring(q.length)
        }

        return other.isEmpty() || other.startsWith('#')
    }

    override fun hashCode(): Int {
        var h = 0
        val s = scheme()
        if (s != null) {
            h = 31 * h + s.hashCode()
            h = 31 * h + 0xFF
        }
        val a = authority()
        if (a != null) {
            h = 31 * h + a.hashCode()
        }
        h = 31 * h + path().hashCode()
        val q = query()
        if (q != null) {
            h = 31 * h + '?'.code
            h = 31 * h + q.hashCode()
        }
        return h
    }

    companion object {
        fun builder(): Builder = Builder.new()

        fun default(): Uri =
            Uri(
                scheme = Scheme.empty(),
                authority = Authority.empty(),
                pathAndQuery = PathAndQuery.slash(),
            )

        fun fromParts(src: Parts): Result<Uri> {
            if (src.scheme != null) {
                if (src.authority == null) {
                    return Result.failure(InvalidUriParts.of(ErrorKind.AuthorityMissing))
                }
                if (src.pathAndQuery == null) {
                    return Result.failure(InvalidUriParts.of(ErrorKind.PathAndQueryMissing))
                }
            } else if (src.authority != null && src.pathAndQuery != null) {
                return Result.failure(InvalidUriParts.of(ErrorKind.SchemeMissing))
            }

            val scheme = src.scheme ?: Scheme.empty()
            val authority = src.authority ?: Authority.empty()
            val pathAndQuery = src.pathAndQuery ?: PathAndQuery.empty()

            return Result.success(
                Uri(
                    scheme = scheme,
                    authority = authority,
                    pathAndQuery = pathAndQuery,
                ),
            )
        }

        fun fromStatic(src: String): Uri =
            fromShared(Bytes.fromStatic(src)).getOrElse {
                throw IllegalArgumentException("static str is not valid URI: $it")
            }

        internal fun fromShared(s: Bytes): Result<Uri> {
            if (s.len() > MAX_LEN) {
                return Result.failure(InvalidUri.of(ErrorKind.TooLong))
            }

            when (s.len()) {
                0 -> return Result.failure(InvalidUri.of(ErrorKind.Empty))
                1 -> when (s.asRef()[0]) {
                    '/'.code.toByte() -> return Result.success(
                        Uri(
                            scheme = Scheme.empty(),
                            authority = Authority.empty(),
                            pathAndQuery = PathAndQuery.slash(),
                        ),
                    )
                    '*'.code.toByte() -> return Result.success(
                        Uri(
                            scheme = Scheme.empty(),
                            authority = Authority.empty(),
                            pathAndQuery = PathAndQuery.star(),
                        ),
                    )
                    else -> {
                        val authority = Authority.fromShared(s).getOrElse { return Result.failure(it) }
                        return Result.success(
                            Uri(
                                scheme = Scheme.empty(),
                                authority = authority,
                                pathAndQuery = PathAndQuery.empty(),
                            ),
                        )
                    }
                }
            }

            if (s.asRef()[0] == '/'.code.toByte()) {
                val pq = PathAndQuery.fromShared(s).getOrElse { return Result.failure(it) }
                return Result.success(
                    Uri(
                        scheme = Scheme.empty(),
                        authority = Authority.empty(),
                        pathAndQuery = pq,
                    ),
                )
            }

            return parseFull(s)
        }

        private fun parseFull(bytes: Bytes): Result<Uri> {
            var s = bytes
            val raw = s.asRef()

            val schemeParse = Scheme2.parse(raw).getOrElse { return Result.failure(it) }
            val scheme: Scheme2 = when (schemeParse) {
                is SchemeParseResult.None -> Scheme2.None
                is SchemeParseResult.Standard -> {
                    s = s.slice(schemeParse.protocol.len() + 3, s.len())
                    Scheme2.Standard(schemeParse.protocol)
                }
                is SchemeParseResult.Other -> {
                    val schemeBytes = s.slice(0, schemeParse.length)
                    s = s.slice(schemeParse.length + 3, s.len())
                    Scheme2.Other(ByteStr.fromUtf8Unchecked(schemeBytes))
                }
            }

            val authorityEnd = Authority.parse(s.asRef()).getOrElse { return Result.failure(it) }

            if (scheme is Scheme2.None) {
                if (authorityEnd != s.len()) {
                    return Result.failure(InvalidUri.of(ErrorKind.InvalidFormat))
                }
                val authority = Authority(ByteStr.fromUtf8Unchecked(s))
                return Result.success(
                    Uri(
                        scheme = Scheme(scheme),
                        authority = authority,
                        pathAndQuery = PathAndQuery.empty(),
                    ),
                )
            }

            if (authorityEnd == 0) {
                return Result.failure(InvalidUri.of(ErrorKind.InvalidFormat))
            }

            val authorityBytes = s.slice(0, authorityEnd)
            s = s.slice(authorityEnd, s.len())
            val authority = Authority(ByteStr.fromUtf8Unchecked(authorityBytes))
            val pq = PathAndQuery.fromShared(s).getOrElse { return Result.failure(it) }

            return Result.success(
                Uri(
                    scheme = Scheme(scheme),
                    authority = authority,
                    pathAndQuery = pq,
                ),
            )
        }

        fun tryFrom(bytes: ByteArray): Result<Uri> =
            fromShared(Bytes.copyFromSlice(bytes))

        fun tryFrom(s: String): Result<Uri> = tryFrom(s.encodeToByteArray())

        fun fromStr(s: String): Result<Uri> = tryFrom(s)
    }
}
