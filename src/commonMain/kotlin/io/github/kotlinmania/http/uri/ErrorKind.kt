// port-lint: source uri/mod.rs
package io.github.kotlinmania.http.uri

/**
 * Internal classification of URI parse failures.
 *
 * Each variant maps to a fixed human-readable phrase via [describe]; that
 * phrase is what [InvalidUri.message] surfaces to callers. The variants are
 * the full set produced by the parser in `uri/mod.rs`, `uri/scheme.rs`,
 * `uri/authority.rs`, `uri/path.rs`, `uri/port.rs`, and `uri/builder.rs`.
 */
internal enum class ErrorKind {
    InvalidUriChar,
    InvalidScheme,
    InvalidAuthority,
    InvalidPort,
    InvalidFormat,
    SchemeMissing,
    AuthorityMissing,
    PathAndQueryMissing,
    TooLong,
    Empty,
    SchemeTooLong,
    ;

    fun describe(): String =
        when (this) {
            InvalidUriChar -> "invalid uri character"
            InvalidScheme -> "invalid scheme"
            InvalidAuthority -> "invalid authority"
            InvalidPort -> "invalid port"
            InvalidFormat -> "invalid format"
            SchemeMissing -> "scheme missing"
            AuthorityMissing -> "authority missing"
            PathAndQueryMissing -> "path missing"
            TooLong -> "uri too long"
            Empty -> "empty string"
            SchemeTooLong -> "scheme too long"
        }
}
