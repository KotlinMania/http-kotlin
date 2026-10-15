@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.http.uri

/**
 * An error resulting from a failed attempt to construct a URI from its
 * individual `Parts` pieces (e.g. via `Uri.fromParts(...)`). Distinct from
 * [InvalidUri] because Rust's `From<ErrorKind>` impls disambiguate the two
 * error surfaces at the type level; the Kotlin port preserves that
 * distinction by wrapping an [InvalidUri] inside [InvalidUriParts] rather
 * than collapsing them.
 *
 */

class InvalidUriParts internal constructor(
    internal val inner: InvalidUri,
) : IllegalArgumentException(inner.message) {
    override fun equals(other: Any?): Boolean =
        other is InvalidUriParts && other.inner == inner

    override fun hashCode(): Int = inner.hashCode()

    companion object {

        internal fun of(kind: ErrorKind): InvalidUriParts =
            InvalidUriParts(InvalidUri.of(kind))
    }
}
