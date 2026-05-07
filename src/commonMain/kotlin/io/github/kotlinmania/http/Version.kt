// port-lint: source src/version.rs
package io.github.kotlinmania.http

/**
 * HTTP version.
 *
 * This module contains a definition of the `Version` type. The `Version`
 * type is intended to be accessed through the root of the crate
 * (`Version`) rather than this module.
 *
 * The `Version` type contains constants that represent the various versions
 * of the HTTP protocol.
 *
 * # Examples
 *
 * ```
 * val http11 = Version.HTTP_11
 * val http2 = Version.HTTP_2
 * check(http11 != http2)
 *
 * println(http2)
 * ```
 */

/** Represents a version of the HTTP spec. */
class Version private constructor(
    private val http: Http,
) : Comparable<Version> {
    companion object {
        /** `HTTP/0.9` */
        val HTTP_09: Version = Version(Http.Http09)

        /** `HTTP/1.0` */
        val HTTP_10: Version = Version(Http.Http10)

        /** `HTTP/1.1` */
        val HTTP_11: Version = Version(Http.Http11)

        /** `HTTP/2.0` */
        val HTTP_2: Version = Version(Http.H2)

        /** `HTTP/3.0` */
        val HTTP_3: Version = Version(Http.H3)

        fun default(): Version = HTTP_11
    }

    override fun compareTo(other: Version): Int = http.compareTo(other.http)

    override fun equals(other: Any?): Boolean = other is Version && http == other.http

    override fun hashCode(): Int = http.hashCode()

    override fun toString(): String =
        when (http) {
            Http.Http09 -> "HTTP/0.9"
            Http.Http10 -> "HTTP/1.0"
            Http.Http11 -> "HTTP/1.1"
            Http.H2 -> "HTTP/2.0"
            Http.H3 -> "HTTP/3.0"
            Http.NonExhaustive -> throw IllegalStateException("unreachable")
        }
}

private enum class Http {
    Http09,
    Http10,
    Http11,
    H2,
    H3,
    NonExhaustive,
}
