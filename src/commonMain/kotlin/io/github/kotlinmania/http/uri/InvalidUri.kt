@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.http.uri

/**
 * An error resulting from a failed attempt to construct a URI.
 *
 * The Rust upstream stores the originating [ErrorKind] in a tuple-struct
 * field; the Kotlin port keeps the same internal split. Public callers only
 * see [message] and the type identity.
 *
 */

class InvalidUri internal constructor(
    internal val kind: ErrorKind,
) : IllegalArgumentException(kind.describe()) {
    override fun equals(other: Any?): Boolean =
        other is InvalidUri && other.kind == kind

    override fun hashCode(): Int = kind.hashCode()

    companion object {

        internal fun of(kind: ErrorKind): InvalidUri = InvalidUri(kind)
    }
}
