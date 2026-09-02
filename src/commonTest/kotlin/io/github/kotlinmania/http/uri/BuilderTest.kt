// port-lint: tests uri/builder.rs
package io.github.kotlinmania.http.uri

import kotlin.test.Test
import kotlin.test.assertEquals

class BuilderTest {
    @Test
    fun buildFromStr() {
        val uri = Builder.new()
            .scheme(Scheme.HTTP)
            .authority("hyper.rs")
            .pathAndQuery("/foo?a=1")
            .build()
            .getOrThrow()
        assertEquals("http", uri.schemeStr())
        assertEquals("hyper.rs", uri.authority()?.host())
        assertEquals("/foo", uri.path())
        assertEquals("a=1", uri.query())
    }

    @Test
    fun buildFromString() {
        for (i in 1 until 10) {
            val uri = Builder.new()
                .pathAndQuery("/foo?a=$i")
                .build()
                .getOrThrow()
            val expectedQuery = "a=$i"
            assertEquals("/foo", uri.path())
            assertEquals(expectedQuery, uri.query())
        }
    }

    @Test
    fun buildFromUri() {
        val originalUri = Uri.default()
        val uri = Builder.from(originalUri).build().getOrThrow()
        assertEquals(originalUri, uri)
    }
}
