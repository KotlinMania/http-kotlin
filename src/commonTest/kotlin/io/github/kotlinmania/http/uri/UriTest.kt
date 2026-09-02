// port-lint: tests uri/tests.rs
package io.github.kotlinmania.http.uri

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UriTest {
    @Test
    fun testCharTable() {
        for (i in URI_CHARS.indices) {
            val v = URI_CHARS[i].toInt() and 0xFF
            if (v != 0) {
                assertEquals(i, v)
            }
        }
    }

    @Test
    fun testUriParsePathAndQuery() {
        testParse(
            "/some/path/here?and=then&hello#and-bye",
            emptyList(),
            expectedScheme = null,
            expectedAuthority = null,
            expectedPath = "/some/path/here",
            expectedQuery = "and=then&hello",
            expectedHost = null,
            expectedPort = null,
        )
    }

    @Test
    fun testUriParseAbsoluteForm() {
        testParse(
            "http://127.0.0.1:61761/chunks",
            emptyList(),
            expectedScheme = "http",
            expectedAuthority = "127.0.0.1:61761",
            expectedPath = "/chunks",
            expectedQuery = null,
            expectedHost = "127.0.0.1",
            expectedPort = 61761,
        )
    }

    @Test
    fun testUriParseAbsoluteFormWithoutPath() {
        testParse(
            "https://127.0.0.1:61761",
            listOf("https://127.0.0.1:61761/"),
            expectedScheme = "https",
            expectedAuthority = "127.0.0.1:61761",
            expectedPath = "/",
            expectedQuery = null,
            expectedHost = "127.0.0.1",
            expectedPort = 61761,
        )
    }

    @Test
    fun testUriParseAsteriskForm() {
        testParse(
            "*",
            emptyList(),
            expectedScheme = null,
            expectedAuthority = null,
            expectedPath = "*",
            expectedQuery = null,
            expectedHost = null,
            expectedPort = null,
        )
    }

    @Test
    fun testUriParseAuthorityNoPort() {
        testParse(
            "localhost",
            listOf("LOCALHOST", "LocaLHOSt"),
            expectedScheme = null,
            expectedAuthority = "localhost",
            expectedPath = "",
            expectedQuery = null,
            expectedHost = "localhost",
            expectedPort = null,
        )
    }

    @Test
    fun testUriAuthorityOnlyOneCharacterIssue197() {
        testParse(
            "S",
            emptyList(),
            expectedScheme = null,
            expectedAuthority = "S",
            expectedPath = "",
            expectedQuery = null,
            expectedHost = "S",
            expectedPort = null,
        )
    }

    @Test
    fun testUriParseAuthorityForm() {
        testParse(
            "localhost:3000",
            listOf("localhosT:3000"),
            expectedScheme = null,
            expectedAuthority = "localhost:3000",
            expectedPath = "",
            expectedQuery = null,
            expectedHost = "localhost",
            expectedPort = 3000,
        )
    }

    @Test
    fun testUriParseAbsoluteWithDefaultPortHttp() {
        testParse(
            "http://127.0.0.1:80",
            listOf("http://127.0.0.1:80/"),
            expectedScheme = "http",
            expectedAuthority = "127.0.0.1:80",
            expectedPath = "/",
            expectedQuery = null,
            expectedHost = "127.0.0.1",
            expectedPort = 80,
        )
    }

    @Test
    fun testUriParseAbsoluteWithDefaultPortHttps() {
        testParse(
            "https://127.0.0.1:443",
            listOf("https://127.0.0.1:443/"),
            expectedScheme = "https",
            expectedAuthority = "127.0.0.1:443",
            expectedPath = "/",
            expectedQuery = null,
            expectedHost = "127.0.0.1",
            expectedPort = 443,
        )
    }

    @Test
    fun testUriParseFragmentQuestionmark() {
        testParse(
            "http://127.0.0.1/#?",
            emptyList(),
            expectedScheme = "http",
            expectedAuthority = "127.0.0.1",
            expectedPath = "/",
            expectedQuery = null,
            expectedHost = "127.0.0.1",
            expectedPort = null,
        )
    }

    @Test
    fun testUriParsePathWithTerminatingQuestionmark() {
        testParse(
            "http://127.0.0.1/path?",
            emptyList(),
            expectedScheme = "http",
            expectedAuthority = "127.0.0.1",
            expectedPath = "/path",
            expectedQuery = "",
            expectedHost = "127.0.0.1",
            expectedPort = null,
        )
    }

    @Test
    fun testUriParseAbsoluteFormWithEmptyPathAndNonemptyQuery() {
        testParse(
            "http://127.0.0.1?foo=bar",
            emptyList(),
            expectedScheme = "http",
            expectedAuthority = "127.0.0.1",
            expectedPath = "/",
            expectedQuery = "foo=bar",
            expectedHost = "127.0.0.1",
            expectedPort = null,
        )
    }

    @Test
    fun testUriParseAbsoluteFormWithEmptyPathAndFragmentWithSlash() {
        testParse(
            "http://127.0.0.1#foo/bar",
            emptyList(),
            expectedScheme = "http",
            expectedAuthority = "127.0.0.1",
            expectedPath = "/",
            expectedQuery = null,
            expectedHost = "127.0.0.1",
            expectedPort = null,
        )
    }

    @Test
    fun testUriParseAbsoluteFormWithEmptyPathAndFragmentWithQuestionmark() {
        testParse(
            "http://127.0.0.1#foo?bar",
            emptyList(),
            expectedScheme = "http",
            expectedAuthority = "127.0.0.1",
            expectedPath = "/",
            expectedQuery = null,
            expectedHost = "127.0.0.1",
            expectedPort = null,
        )
    }

    @Test
    fun testUriParseLongHostWithNoScheme() {
        testParse(
            "thequickbrownfoxjumpedoverthelazydogtofindthelargedangerousdragon.localhost",
            emptyList(),
            expectedScheme = null,
            expectedAuthority = "thequickbrownfoxjumpedoverthelazydogtofindthelargedangerousdragon.localhost",
            expectedPath = "",
            expectedQuery = null,
            expectedHost = "thequickbrownfoxjumpedoverthelazydogtofindthelargedangerousdragon.localhost",
            expectedPort = null,
        )
    }

    @Test
    fun testUriParseLongHostWithPortAndNoScheme() {
        testParse(
            "thequickbrownfoxjumpedoverthelazydogtofindthelargedangerousdragon.localhost:1234",
            emptyList(),
            expectedScheme = null,
            expectedAuthority = "thequickbrownfoxjumpedoverthelazydogtofindthelargedangerousdragon.localhost:1234",
            expectedPath = "",
            expectedQuery = null,
            expectedHost = "thequickbrownfoxjumpedoverthelazydogtofindthelargedangerousdragon.localhost",
            expectedPort = 1234,
        )
    }

    @Test
    fun testUserinfo1() {
        testParse(
            "http://a:b@127.0.0.1:1234/",
            emptyList(),
            expectedScheme = "http",
            expectedAuthority = "a:b@127.0.0.1:1234",
            expectedPath = "/",
            expectedQuery = null,
            expectedHost = "127.0.0.1",
            expectedPort = 1234,
        )
    }

    @Test
    fun testUserinfo2() {
        testParse(
            "http://a:b@127.0.0.1/",
            emptyList(),
            expectedScheme = "http",
            expectedAuthority = "a:b@127.0.0.1",
            expectedPath = "/",
            expectedQuery = null,
            expectedHost = "127.0.0.1",
            expectedPort = null,
        )
    }

    @Test
    fun testUserinfo3() {
        testParse(
            "http://a@127.0.0.1/",
            emptyList(),
            expectedScheme = "http",
            expectedAuthority = "a@127.0.0.1",
            expectedPath = "/",
            expectedQuery = null,
            expectedHost = "127.0.0.1",
            expectedPort = null,
        )
    }

    @Test
    fun testUserinfoWithPort() {
        testParse(
            "user@localhost:3000",
            emptyList(),
            expectedScheme = null,
            expectedAuthority = "user@localhost:3000",
            expectedPath = "",
            expectedQuery = null,
            expectedHost = "localhost",
            expectedPort = 3000,
        )
    }

    @Test
    fun testUserinfoPassWithPort() {
        testParse(
            "user:pass@localhost:3000",
            emptyList(),
            expectedScheme = null,
            expectedAuthority = "user:pass@localhost:3000",
            expectedPath = "",
            expectedQuery = null,
            expectedHost = "localhost",
            expectedPort = 3000,
        )
    }

    @Test
    fun testIpv6() {
        testParse(
            "http://[2001:0db8:85a3:0000:0000:8a2e:0370:7334]/",
            emptyList(),
            expectedScheme = "http",
            expectedAuthority = "[2001:0db8:85a3:0000:0000:8a2e:0370:7334]",
            expectedPath = "/",
            expectedQuery = null,
            expectedHost = "[2001:0db8:85a3:0000:0000:8a2e:0370:7334]",
            expectedPort = null,
        )
    }

    @Test
    fun testIpv6Shorthand() {
        testParse(
            "http://[::1]/",
            emptyList(),
            expectedScheme = "http",
            expectedAuthority = "[::1]",
            expectedPath = "/",
            expectedQuery = null,
            expectedHost = "[::1]",
            expectedPort = null,
        )
    }

    @Test
    fun testIpv6Shorthand2() {
        testParse(
            "http://[::]/",
            emptyList(),
            expectedScheme = "http",
            expectedAuthority = "[::]",
            expectedPath = "/",
            expectedQuery = null,
            expectedHost = "[::]",
            expectedPort = null,
        )
    }

    @Test
    fun testIpv6Shorthand3() {
        testParse(
            "http://[2001:db8::2:1]/",
            emptyList(),
            expectedScheme = "http",
            expectedAuthority = "[2001:db8::2:1]",
            expectedPath = "/",
            expectedQuery = null,
            expectedHost = "[2001:db8::2:1]",
            expectedPort = null,
        )
    }

    @Test
    fun testIpv6WithPort() {
        testParse(
            "http://[2001:0db8:85a3:0000:0000:8a2e:0370:7334]:8008/",
            emptyList(),
            expectedScheme = "http",
            expectedAuthority = "[2001:0db8:85a3:0000:0000:8a2e:0370:7334]:8008",
            expectedPath = "/",
            expectedQuery = null,
            expectedHost = "[2001:0db8:85a3:0000:0000:8a2e:0370:7334]",
            expectedPort = 8008,
        )
    }

    @Test
    fun testPercentageEncodedPath() {
        testParse(
            "/echo/abcdefgh_i-j%20/abcdefg_i-j%20478",
            emptyList(),
            expectedScheme = null,
            expectedAuthority = null,
            expectedPath = "/echo/abcdefgh_i-j%20/abcdefg_i-j%20478",
            expectedQuery = null,
            expectedHost = null,
            expectedPort = null,
        )
    }

    @Test
    fun testPathPermissive() {
        val uri = Uri.fromStr("/foo=bar|baz\\^~%").getOrThrow()
        assertEquals("/foo=bar|baz\\^~%", uri.path())
    }

    @Test
    fun testQueryPermissive() {
        val uri = Uri.fromStr("/?foo={bar|baz}\\^`").getOrThrow()
        assertEquals("foo={bar|baz}\\^`", uri.query())
    }

    @Test
    fun testUriParseError() {
        fun err(s: String) {
            assertTrue(Uri.fromStr(s).isFailure, "Expected failure for $s")
        }

        err("http://")
        err("htt:p//host")
        err("hyper.rs/")
        err("hyper.rs?key=val")
        err("?key=val")
        err("localhost/")
        err("localhost?key=val")
        err("\u0000")
        err("http://[::1")
        err("http://::1]")
        err("localhost:8080:3030")
        err("@")
        err("http://username:password@/wut")

        // illegal queries
        err("/?foo\rbar")
        err("/?foo\nbar")
        err("/?<")
        err("/?>")
    }

    @Test
    fun testMaxUriLen() {
        val sb = StringBuilder("http://localhost/")
        repeat(70 * 1024) { sb.append('a') }
        val res = Uri.fromStr(sb.toString())
        assertTrue(res.isFailure)
        assertEquals(InvalidUri.of(ErrorKind.TooLong), res.exceptionOrNull())
    }

    @Test
    fun testOverflowingScheme() {
        val sb = StringBuilder()
        repeat(256) { sb.append('a') }
        sb.append("://localhost/")
        val res = Uri.fromStr(sb.toString())
        assertTrue(res.isFailure)
        assertEquals(InvalidUri.of(ErrorKind.SchemeTooLong), res.exceptionOrNull())
    }

    @Test
    fun testMaxLengthScheme() {
        val sb = StringBuilder()
        repeat(64) { sb.append('a') }
        sb.append("://localhost/")
        val uri = Uri.fromStr(sb.toString()).getOrThrow()
        assertEquals(64, uri.schemeStr()?.length)
    }

    @Test
    fun testUriToPathAndQuery() {
        val cases = listOf(
            "/" to "/",
            "/foo?bar" to "/foo?bar",
            "/foo?bar#nope" to "/foo?bar",
            "http://hyper.rs" to "/",
            "http://hyper.rs/" to "/",
            "http://hyper.rs/path" to "/path",
            "http://hyper.rs?query" to "/?query",
            "*" to "*",
        )

        for ((input, expected) in cases) {
            val uri = Uri.fromStr(input).getOrThrow()
            val s = uri.pathAndQuery()?.toString()
            assertEquals(expected, s)
        }
    }

    @Test
    fun testAuthorityUriPartsRoundTrip() {
        val s = "hyper.rs"
        val uri = Uri.fromStr(s).getOrThrow()
        assertTrue(uri.eq(s))
        assertEquals(s, uri.toString())

        val parts = uri.intoParts()
        val uri2 = Uri.fromParts(parts).getOrThrow()
        assertTrue(uri2.eq(s))
        assertEquals(s, uri2.toString())
    }

    @Test
    fun testPartialEqPathWithTerminatingQuestionmark() {
        val a = "/path"
        val uri = Uri.fromStr("/path?").getOrThrow()
        assertTrue(uri.eq(a))
    }

    private fun testParse(
        origStr: String,
        alt: List<String>,
        expectedScheme: String?,
        expectedAuthority: String?,
        expectedPath: String,
        expectedQuery: String?,
        expectedHost: String?,
        expectedPort: Int?,
    ) {
        val uri = Uri.fromStr(origStr).getOrThrow()
        if (expectedScheme != null) {
            assertEquals(expectedScheme, uri.schemeStr())
        } else {
            assertNull(uri.scheme())
        }

        if (expectedAuthority != null) {
            assertEquals(expectedAuthority, uri.authority()?.asStr())
        } else {
            assertNull(uri.authority())
        }

        assertEquals(expectedPath, uri.path())
        assertEquals(expectedQuery, uri.query())
        assertEquals(expectedHost, uri.host())
        assertEquals(expectedPort, uri.portU16())

        assertTrue(uri.eq(origStr), "partial eq to original str")

        val newStr = uri.toString()
        val newUri = Uri.fromStr(newStr).getOrThrow()
        assertTrue(newUri.eq(origStr), "round trip still equals original str")

        for (a in alt) {
            val other = Uri.fromStr(a).getOrThrow()
            assertTrue(uri.eq(a))
            assertEquals(uri, other)
        }
    }
}
