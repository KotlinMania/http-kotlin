// port-lint: source method.rs
package io.github.kotlinmania.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MethodTest {
    @Test
    fun testMethodEq() {
        assertEquals(Method.GET, Method.GET)
        assertTrue(Method.GET.eq("GET"))
        assertTrue(Method.GET.eq(Method.GET))
        assertEquals("GET", Method.GET.asRef())
        assertEquals(Method.GET, Method.from(Method.GET))
    }

    @Test
    fun testInvalidMethod() {
        assertTrue(Method.fromStr("").isFailure)
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
        val wow = Method.fromStr("WOW").getOrThrow()
        assertTrue(wow.eq("WOW"))
        assertEquals("WOW", wow.asStr())

        val mixedCase = Method.fromStr("wOw!!").getOrThrow()
        assertTrue(mixedCase.eq("wOw!!"))
        assertEquals("wOw!!", mixedCase.asStr())

        val longMethod = "This_is_a_very_long_method.It_is_valid_but_unlikely."
        val parsedLongMethod = Method.fromStr(longMethod).getOrThrow()
        assertTrue(parsedLongMethod.eq(longMethod))
        assertEquals(longMethod, parsedLongMethod.asStr())

        val longestInlineMethod = ByteArray(15) { 'A'.code.toByte() }
        assertEquals(
            "AAAAAAAAAAAAAAA",
            Method.fromBytes(longestInlineMethod).getOrThrow().asStr(),
        )

        val shortestAllocatedMethod = ByteArray(16) { 'A'.code.toByte() }
        assertEquals(
            "AAAAAAAAAAAAAAAA",
            Method.fromBytes(shortestAllocatedMethod).getOrThrow().asStr(),
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
                Method.fromStr(method).getOrThrow().asStr(),
                "testing $c is a valid method character",
            )
            assertTrue(
                Method.fromStr(method).getOrThrow().eq(method),
                "testing $c is a valid method character",
            )
        }
    }
}
