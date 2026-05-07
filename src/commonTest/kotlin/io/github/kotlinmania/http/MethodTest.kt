// port-lint: source src/method.rs
package io.github.kotlinmania.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MethodTest {
    @Test
    fun testMethodEq() {
        assertEquals(Method.GET, Method.GET)
        assertEquals("GET", Method.GET.asString())
        assertEquals(Method.GET, Method.from(Method.GET))
    }

    @Test
    fun testInvalidMethod() {
        assertTrue(Method.parse("").isFailure)
        assertTrue(Method.fromBytes(ByteArray(0)).isFailure)
        assertTrue(Method.fromBytes(byteArrayOf(0xc0.toByte())).isFailure)
        assertTrue(Method.fromBytes(byteArrayOf(0x10.toByte())).isFailure)
    }

    @Test
    fun testIsIdempotent() {
        assertTrue(Method.OPTIONS.isIdempotent())
        assertTrue(Method.GET.isIdempotent())
        assertTrue(Method.PUT.isIdempotent())
        assertTrue(Method.DELETE.isIdempotent())
        assertTrue(Method.HEAD.isIdempotent())
        assertTrue(Method.TRACE.isIdempotent())

        assertFalse(Method.POST.isIdempotent())
        assertFalse(Method.CONNECT.isIdempotent())
        assertFalse(Method.PATCH.isIdempotent())
    }

    @Test
    fun testExtensionMethod() {
        assertEquals("WOW", Method.parse("WOW").getOrThrow().asString())
        assertEquals("wOw!!", Method.parse("wOw!!").getOrThrow().asString())

        val longMethod = "This_is_a_very_long_method.It_is_valid_but_unlikely."
        assertEquals(longMethod, Method.parse(longMethod).getOrThrow().asString())

        val longestInlineMethod = ByteArray(15) { 'A'.code.toByte() }
        assertEquals(
            "AAAAAAAAAAAAAAA",
            Method.fromBytes(longestInlineMethod).getOrThrow().asString(),
        )

        val shortestAllocatedMethod = ByteArray(16) { 'A'.code.toByte() }
        assertEquals(
            "AAAAAAAAAAAAAAAA",
            Method.fromBytes(shortestAllocatedMethod).getOrThrow().asString(),
        )
    }

    @Test
    fun testExtensionMethodChars() {
        val validMethodChars =
            "!#$%&'*+-.^_`|~0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

        for (c in validMethodChars) {
            val method = c.toString()

            assertEquals(
                method,
                Method.parse(method).getOrThrow().asString(),
                "testing $c is a valid method character",
            )
        }
    }
}
