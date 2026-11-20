package io.github.kotlinmania.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame

class MethodCloneParityTest {
    private fun check(text: String) {
        val original = Method.fromStr(text).getOrThrow()
        val clone = original.clone()
        assertEquals(original, clone)
        assertNotSame(original, clone)
        assertEquals(original.hashCode(), clone.hashCode())
        assertEquals(original.isSafe(), clone.isSafe())
        assertEquals(original.isIdempotent(), clone.isIdempotent())
        assertEquals(text, clone.asRef())
        assertEquals(text, clone.toString())
    }

    @Test
    fun standardMethods() {
        for (text in listOf("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "CONNECT", "PATCH", "TRACE")) check(text)
    }

    @Test
    fun inlineExtensions() {
        for (text in listOf("x", "GE", "get", "CUSTOM", "123456789012345")) check(text)
    }

    @Test
    fun allocatedExtensions() {
        for (length in listOf(16, 17, 64, 256)) check("X".repeat(length))
    }

    @Test
    fun caseSensitive() {
        for ((upper, lower) in listOf("GET" to "get", "POST" to "post", "CUSTOM" to "custom")) {
            val a = Method.fromStr(upper).getOrThrow()
            val b = Method.fromStr(lower).getOrThrow()
            assertNotEquals(a.clone(), b.clone())
            assertEquals(upper, a.clone().asRef())
            assertEquals(lower, b.clone().asRef())
        }
    }

    @Test
    fun byteDomain() {
        for (byte in 0..255) {
            val allowed = byte in '0'.code..'9'.code || byte in 'A'.code..'Z'.code || byte in 'a'.code..'z'.code || byte.toChar() in "!#$%&'*+-.^_`|~"
            val result = Method.fromBytes(byteArrayOf(byte.toByte()))
            assertEquals(allowed, result.isSuccess, "byte=$byte")
            if (result.isSuccess) {
                val original = result.getOrThrow()
                val clone = original.clone()
                assertEquals(original, clone)
                assertEquals(byte.toChar().toString(), clone.asRef())
            }
        }
    }

    @Test
    fun inputCopyIndependence() {
        for (text in listOf("GET", "custom", "XXXXXXXXXXXXXXXX")) {
            val input = text.encodeToByteArray()
            val original = Method.fromBytes(input).getOrThrow()
            val clone = original.clone()
            input[0] = 'Z'.code.toByte()
            assertEquals(text, original.asRef())
            assertEquals(text, clone.asRef())
        }
    }

    @Test
    fun cloneOfClone() {
        for (length in listOf(1, 15, 16, 32)) {
            val text = "X".repeat(length)
            val original = Method.fromStr(text).getOrThrow()
            val clone = original.clone().clone()
            assertEquals(original, clone)
            assertEquals(text, clone.asRef())
            assertEquals(original.hashCode(), clone.hashCode())
        }
    }
}
