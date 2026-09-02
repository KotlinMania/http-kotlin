// port-lint: source uri/mod.rs
package io.github.kotlinmania.http.uri

/**
 * The various parts of a URI.
 *
 * This struct is used to provide to and retrieve from a URI.
 */
class Parts(
    var scheme: Scheme? = null,
    var authority: Authority? = null,
    var pathAndQuery: PathAndQuery? = null,
) {
    override fun toString(): String =
        "Parts(scheme=$scheme, authority=$authority, pathAndQuery=$pathAndQuery)"
}
