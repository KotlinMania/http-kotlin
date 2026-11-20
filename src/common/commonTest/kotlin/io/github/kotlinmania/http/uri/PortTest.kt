// port-lint: source uri/port.rs
package io.github.kotlinmania.http.uri

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PortTest {
    @Test
    fun partialeqPort() {
        val portA = Port.fromStr("8080").getOrThrow()
        val portB = Port.fromStr("8080").getOrThrow()
        assertEquals(portA, portB)
    }

    // Upstream `partialeq_port_different_reprs` constructs two `Port<T>` instances with
    // different generic `T` (one `&str`, one `String`) and verifies they compare equal.
    // Kotlin has no generic `Port<T>` — `String` covers both the borrowed and owned roles
    // — so the upstream test reduces to the same case as `partialeqPort` above. There is
    // no Kotlin invariant to assert here that is distinct from `partialeqPort`; the test
    // is therefore unported. See `uri/Port.kt` for the rationale on dropping the type
    // parameter.

    @Test
    fun partialeqU16() {
        val port = Port.fromStr("8080").getOrThrow()
        // Symmetric equality with a raw port number. Kotlin's `==` always
        // dispatches to `equals(Any?)` which requires identical declared
        // types, so the `200 == status` direction of the upstream
        // `assert_eq!(8080, port)` is expressed via `port.eq(Int)` — the
        // same idiom `StatusCodeTest.equatesWithU16` uses.
        assertTrue(port.eq(8080))
        assertTrue(port.asU16() == 8080)
    }

    @Test
    fun u16FromPort() {
        val port = Port.fromStr("8080").getOrThrow()
        assertEquals(8080, port.asU16())
    }
}
