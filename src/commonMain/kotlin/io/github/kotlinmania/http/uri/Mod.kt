// port-lint: source uri/mod.rs
package io.github.kotlinmania.http.uri

/**
 * URI component types.
 *
 * The module provides [Uri], [Scheme], [Authority], [PathAndQuery], [Port],
 * and the [Builder] used to assemble them, plus the [InvalidUri] /
 * [InvalidUriParts] error types they share.
 *
 * # `Uri`
 *
 * `Uri` is the central type. It represents a parsed Uniform Resource
 * Identifier as defined by [RFC 3986][rfc3986] and supports the absolute,
 * absolute-path, authority-form, and asterisk-form variants the HTTP
 * specifications permit on the request line. Components can be accessed via
 * the [Uri.scheme], [Uri.authority], [Uri.pathAndQuery], [Uri.host], and
 * [Uri.port] accessors.
 *
 * # `Scheme`, `Authority`, `PathAndQuery`, `Port`
 *
 * Each of those four types is a standalone view onto one component of a
 * `Uri` (or onto a stand-alone fragment parsed in isolation). They share
 * the [InvalidUri] error type for parse failures.
 *
 * # `Builder`
 *
 * [Builder] assembles a `Uri` from its components incrementally, returning
 * `Result<Uri>` from [Builder.build].
 *
 * [rfc3986]: https://datatracker.ietf.org/doc/html/rfc3986
 */

// mod authority;
// mod builder;
// mod path;
// mod port;
// mod scheme;
//
// Each of the modules above becomes a sibling Kotlin file in this package.

// pub use self::authority::Authority;
// pub use self::builder::Builder;
// pub use self::path::PathAndQuery;
// pub use self::port::Port;
// pub use self::scheme::Scheme;
//
// Per the workspace mod.rs re-export workflow, this file does not introduce a central alias
// for any of the re-exported names. Callers should reference the symbols at their original
// Kotlin location (the ports of `uri/authority.rs`, `uri/builder.rs`, `uri/path.rs`,
// `uri/port.rs`, `uri/scheme.rs` in this same package) and use Kotlin import aliasing if
// they need to preserve a particular identifier.
//
// Callers migrated:
// - none
//
// The free constants from `uri/mod.rs` (`MAX_LEN`, `URI_CHARS`) live in their own
// per-symbol files alongside the types that depend on them once those types port.

/**
 * `u16::MAX - 1` — the maximum URI length the parser will accept.
 *
 * The full `u16::MAX` value is reserved as a sentinel for absent offsets in
 * the parsed-URI representation, so the practical length cap is one short of
 * the full range.
 */
internal const val MAX_LEN: Int = (1 shl 16) - 2
