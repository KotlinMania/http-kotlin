// port-lint: source header/mod.rs
package io.github.kotlinmania.http.header

/**
 * HTTP header types
 *
 * The module provides [HeaderName], [HeaderMap], and a number of types
 * used for interacting with `HeaderMap`. These types allow representing both
 * HTTP/1 and HTTP/2 headers.
 *
 * # `HeaderName`
 *
 * The `HeaderName` type represents both standard header names as well as
 * custom header names. The type handles the case insensitive nature of header
 * names and is used as the key portion of `HeaderMap`. Header names are
 * normalized to lower case. In other words, when creating a `HeaderName` with
 * a string, even if upper case characters are included, when getting a string
 * representation of the `HeaderName`, it will be all lower case. This allows
 * for faster `HeaderMap` comparison operations.
 *
 * The internal representation is optimized to efficiently handle the cases
 * most commonly encountered when working with HTTP. Standard header names are
 * special cased and are represented internally as an enum. Short custom
 * headers will be stored directly in the `HeaderName` struct and will not
 * incur any allocation overhead, however longer strings will require an
 * allocation for storage.
 *
 * ## Limitations
 *
 * `HeaderName` has a max length of 32,768 for header names. Attempting to
 * parse longer names will result in a panic.
 *
 * # `HeaderMap`
 *
 * The [HeaderMap] type is a specialized
 * [multimap](https://en.wikipedia.org/wiki/Multimap) structure for storing
 * header names and values. It is designed specifically for efficient
 * manipulation of HTTP headers. It supports multiple values per header name
 * and provides specialized APIs for insertion, retrieval, and iteration.
 *
 * [*See also the `HeaderMap` type.*](HeaderMap)
 */

// mod map;
// mod name;
// mod value;
//
// Each of the modules above becomes a sibling Kotlin file in this package.

// pub use self::map::{
//     AsHeaderName, Drain, Entry, GetAll, HeaderMap, IntoHeaderName, IntoIter, Iter, IterMut, Keys,
//     MaxSizeReached, OccupiedEntry, VacantEntry, ValueDrain, ValueIter, ValueIterMut, Values,
//     ValuesMut,
// };
// pub use self::name::{HeaderName, InvalidHeaderName};
// pub use self::value::{HeaderValue, InvalidHeaderValue, ToStrError};
//
// Per the workspace `mod.rs` re-export workflow, this file does not introduce a central alias
// for any of the re-exported names. Callers should reference the symbols at their original
// Kotlin location (the ports of `header/map.rs`, `header/name.rs`, and `header/value.rs` in this
// same package) and use Kotlin import aliasing if they need to preserve a particular identifier.
//
// Callers migrated:
// - none
//
// The standard header-name constants (ACCEPT, ACCEPT_CHARSET, ..., X_XSS_PROTECTION) are
// re-exported here from `name.rs`. Callers should import them directly from the port of
// `name.rs`.

/**
 * Maximum length of a header name
 *
 * Generally, 64kb for a header name is WAY too much than would ever be needed
 * in practice. Restricting it to this size enables using `UShort` values to
 * represent offsets when dealing with header names.
 */
internal const val MAX_HEADER_NAME_LEN: Int = (1 shl 16) - 1
