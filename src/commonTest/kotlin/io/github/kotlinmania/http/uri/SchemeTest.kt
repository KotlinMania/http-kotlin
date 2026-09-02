// port-lint: tests uri/scheme.rs
package io.github.kotlinmania.http.uri

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SchemeTest {
    @Test
    fun schemeEqToStr() {
        assertEquals("http", scheme("http").asStr())
        assertEquals("https", scheme("https").asStr())
        assertEquals("ftp", scheme("ftp").asStr())
        assertEquals("my+funky+scheme", scheme("my+funky+scheme").asStr())
    }

    @Test
    fun invalidSchemeIsError() {
        assertTrue(Scheme.tryFrom("my_funky_scheme").isFailure, "Unexpectedly valid Scheme")
        assertTrue(Scheme.tryFrom(byteArrayOf(0xC0.toByte())).isFailure, "Unexpectedly valid Scheme")
    }

    private fun scheme(s: String): Scheme =
        Scheme.fromStr(s).getOrThrow()
}
