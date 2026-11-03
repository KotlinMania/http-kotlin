package io.github.kotlinmania.http.uri

import io.github.kotlinmania.http.header.HeaderName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AsciiComparisonParityTest {
    @Test
    fun schemeComparisonDoesNotFoldUnicode() {
        for ((ascii, unicode) in listOf("k" to "K", "s" to "ſ", "i" to "ı")) {
            val scheme = Scheme.fromStr(ascii).getOrThrow()
            assertFalse(scheme.eq(unicode))
            assertTrue(scheme.eq(ascii.uppercase()))
        }
    }

    @Test
    fun authorityComparisonDoesNotFoldUnicode() {
        for ((ascii, unicode) in listOf("k.example" to "K.example", "s.example" to "ſ.example", "i.example" to "ı.example")) {
            val authority = Authority.fromStatic(ascii)
            assertFalse(authority.eq(unicode))
            assertFalse(authority.equals(unicode))
            assertTrue(authority.eq(ascii.uppercase()))
        }
    }

    @Test
    fun uriComponentComparisonDoesNotFoldUnicode() {
        val uri = Uri.fromStatic("k://k.example/Path?Q=1")
        assertFalse(uri.eq("K://k.example/Path?Q=1"))
        assertFalse(uri.eq("k://K.example/Path?Q=1"))
        assertTrue(uri.eq("K://K.EXAMPLE/Path?Q=1"))
    }

    @Test
    fun authorityOrderMapsToLowercaseBeforeComparing() {
        for ((left, right) in listOf("[::1]" to "a.example", "_" to "a", "z" to "~")) {
            val authority = Authority.fromStatic(left)
            assertTrue(authority < Authority.fromStatic(right))
            assertTrue(authority.compareTo(right) < 0)
        }
    }

    @Test
    fun authorityOrderUsesUnsignedBytesForUnicodeStrings() {
        val authority = Authority.fromStatic("k.example")
        assertTrue(authority.compareTo("K.example") < 0)
        assertEquals(0, authority.compareTo("K.EXAMPLE"))
    }

    @Test
    fun pathAndQueryCaseAndFragmentSemanticsStayDistinct() {
        val uri = Uri.fromStatic("http://example/Path?Q=1")
        assertFalse(uri.eq("HTTP://EXAMPLE/path?Q=1"))
        assertFalse(uri.eq("HTTP://EXAMPLE/Path?q=1"))
        assertTrue(uri.eq("HTTP://EXAMPLE/Path?Q=1#K"))
    }

    @Test
    fun unicodeSchemeAndAuthorityInputStillFailsParsing() {
        for (text in listOf("K", "ſ", "ı")) {
            assertTrue(Scheme.fromStr(text).isFailure)
            assertTrue(Authority.fromStr(text).isFailure)
        }
    }

    @Test
    fun headerNamesKeepAsciiOnlyComparison() {
        val header = HeaderName.fromStatic("key")
        assertTrue(header.eq("KEY"))
        assertFalse(header.eq("Key"))
    }
}
