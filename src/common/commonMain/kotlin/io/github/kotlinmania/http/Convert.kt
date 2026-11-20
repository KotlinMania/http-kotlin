// port-lint: source convert.rs
package io.github.kotlinmania.http

/**
 * Invokes [block] with [value] cast to [Out] when [value] matches [Out], returning
 * whether the downcast succeeded.
 */
internal inline fun <In, reified Out> downcastIfSame(
    value: In,
    block: (Out) -> Unit,
): Boolean {
    if (value is Out) {
        block(value)
        return true
    }
    return false
}
