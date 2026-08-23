// port-lint: source uri/port.rs
package io.github.kotlinmania.http.uri

import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

/**
 * The port component of a URI.
 *
 * The Rust upstream parameterises `Port<T>` over its string representation
 * (`T: AsRef<str>`) so the parser can hand back either a borrowed `&str` or
 * an owned `String` without copying. Kotlin's `String` already covers both
 * roles (immutable, owned UTF-16), so the Kotlin port is non-generic and
 * carries the original textual representation in [repr] verbatim. The
 * parsed numeric value lives in [value]; the workspace convention is to
 * surface `u16` as Kotlin `Int` (see `StatusCode`), so [value] is `Int`
 * with the caller-visible accessor [asU16] returning the same range.
 *
 * # Examples
 *
 * ```kotlin
 * val port = Port.fromStr("80").getOrThrow()
 * check(port.asU16() == 80)
 * check(port.asStr() == "80")
 * ```
 */
@OptIn(ExperimentalObjCRefinement::class)
class Port private constructor(
    private val value: Int,
    private val repr: String,
) {
    /** Returns the port number. The value is always in the `0..65535` `u16` range. */
    fun asU16(): Int = value

    /** Returns the textual representation the port was parsed from. */
    fun asStr(): String = repr

    /** [AsRef][https://doc.rust-lang.org/std/convert/trait.AsRef.html] equivalent. */
    fun asRef(): String = asStr()

    /**
     * Equal when the parsed numeric value is equal, regardless of the original
     * textual representation. Mirrors the Rust upstream's
     * `impl<T, U> PartialEq<Port<U>> for Port<T>` which compares only the
     * `port: u16` field.
     */
    override fun equals(other: Any?): Boolean =
        other is Port && other.value == value

    override fun hashCode(): Int = value.hashCode()

    /**
     * Display formatting — the numeric value, matching `impl fmt::Display for
     * Port<T>` upstream (which forwards to `u16::fmt` and intentionally drops
     * the textual `repr`).
     */
    override fun toString(): String = value.toString()

    /**
     * Numeric equality with a raw port number. Kotlin can't overload `==`
     * across types, so this is exposed as a named API that mirrors the Rust
     * upstream's `impl PartialEq<u16> for Port<T>` and its symmetric
     * counterpart `impl PartialEq<Port<T>> for u16`.
     */
    fun eq(other: Int): Boolean = value == other

    companion object {
        /**
         * Parses a port number from its textual representation. The supplied
         * string must contain a single valid `u16` decimal literal with no
         * surrounding whitespace, sign, or radix prefix.
         *
         * Returns [Result.success] holding the parsed `Port` on success, or
         * [Result.failure] holding an [InvalidUri] with [ErrorKind.InvalidPort]
         * on parse failure. Mirrors `Port::from_str` upstream, which is
         * `pub(crate)` in Rust; the Kotlin equivalent is exposed publicly so
         * tests outside the module can exercise it directly (the Rust tests
         * exercise it via `super::*`).
         */
        @HiddenFromObjC
        fun fromStr(bytes: String): Result<Port> {
            val parsed =
                bytes.toIntOrNull(10) ?: return Result.failure(
                    InvalidUri.of(ErrorKind.InvalidPort),
                )
            if (parsed !in 0..U16_MAX) {
                return Result.failure(InvalidUri.of(ErrorKind.InvalidPort))
            }
            return Result.success(Port(parsed, bytes))
        }

        private const val U16_MAX: Int = (1 shl 16) - 1
    }
}
