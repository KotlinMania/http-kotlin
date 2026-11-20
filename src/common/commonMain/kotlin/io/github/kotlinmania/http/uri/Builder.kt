// port-lint: source uri/builder.rs
package io.github.kotlinmania.http.uri

/**
 * A builder for [Uri]s.
 */
class Builder internal constructor(
    private val partsResult: Result<Parts>,
) {
    constructor() : this(Result.success(Parts()))

    /**
     * Set the [Scheme] for this URI.
     */
    fun scheme(scheme: Scheme): Builder =
        map { parts ->
            parts.scheme = scheme
            parts
        }

    /**
     * Set the [Scheme] for this URI from a string.
     */
    fun scheme(schemeStr: String): Builder =
        map { parts ->
            val s = Scheme.fromStr(schemeStr).getOrThrow()
            parts.scheme = s
            parts
        }

    /**
     * Set the [Authority] for this URI.
     */
    fun authority(auth: Authority): Builder =
        map { parts ->
            parts.authority = auth
            parts
        }

    /**
     * Set the [Authority] for this URI from a string.
     */
    fun authority(authStr: String): Builder =
        map { parts ->
            val a = Authority.fromStr(authStr).getOrThrow()
            parts.authority = a
            parts
        }

    /**
     * Set the [PathAndQuery] for this URI.
     */
    fun pathAndQuery(pAndQ: PathAndQuery): Builder =
        map { parts ->
            parts.pathAndQuery = pAndQ
            parts
        }

    /**
     * Set the [PathAndQuery] for this URI from a string.
     */
    fun pathAndQuery(pAndQStr: String): Builder =
        map { parts ->
            val pq = PathAndQuery.fromStr(pAndQStr).getOrThrow()
            parts.pathAndQuery = pq
            parts
        }

    /**
     * Consumes this builder, and tries to construct a valid [Uri] from the configured pieces.
     */
    fun build(): Result<Uri> =
        partsResult.fold(
            onSuccess = { Uri.fromParts(it) },
            onFailure = { Result.failure(it) },
        )

    private inline fun map(crossinline func: (Parts) -> Parts): Builder =
        Builder(
            partsResult.mapCatching { parts ->
                func(parts)
            },
        )

    companion object {
        fun new(): Builder = Builder()

        fun from(uri: Uri): Builder =
            Builder(Result.success(uri.intoParts()))
    }
}
