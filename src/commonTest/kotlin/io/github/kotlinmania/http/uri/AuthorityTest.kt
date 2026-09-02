// port-lint: tests uri/authority.rs
package io.github.kotlinmania.http.uri

import io.github.kotlinmania.bytes.Bytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuthorityTest {
    @Test
    fun parseEmptyStringIsError() {
        val err = Authority.parseNonEmpty(ByteArray(0))
        assertTrue(err.isFailure)
        assertEquals(InvalidUri.of(ErrorKind.Empty), err.exceptionOrNull())
    }

    @Test
    fun equalToSelfOfSameAuthority() {
        val authority1: Authority = Authority.fromStr("example.com").getOrThrow()
        val authority2: Authority = Authority.fromStr("EXAMPLE.COM").getOrThrow()
        assertEquals(authority1, authority2)
        assertEquals(authority2, authority1)
    }

    @Test
    fun notEqualToSelfOfDifferentAuthority() {
        val authority1: Authority = Authority.fromStr("example.com").getOrThrow()
        val authority2: Authority = Authority.fromStr("test.com").getOrThrow()
        assertNotEquals(authority1, authority2)
        assertNotEquals(authority2, authority1)
    }

    @Test
    fun equatesWithAStr() {
        val authority: Authority = Authority.fromStr("example.com").getOrThrow()
        assertTrue(authority.eq("EXAMPLE.com"))
    }

    @Test
    fun fromStaticEquatesWithAStr() {
        val authority = Authority.fromStatic("example.com")
        assertTrue(authority.eq("example.com"))
    }

    @Test
    fun notEqualWithAStrOfADifferentAuthority() {
        val authority: Authority = Authority.fromStr("example.com").getOrThrow()
        assertFalse(authority.eq("test.com"))
    }

    @Test
    fun equatesWithAString() {
        val authority: Authority = Authority.fromStr("example.com").getOrThrow()
        assertTrue(authority.eq("EXAMPLE.com"))
    }

    @Test
    fun equatesWithAStringOfADifferentAuthority() {
        val authority: Authority = Authority.fromStr("example.com").getOrThrow()
        assertFalse(authority.eq("test.com"))
    }

    @Test
    fun comparesToSelf() {
        val authority1: Authority = Authority.fromStr("abc.com").getOrThrow()
        val authority2: Authority = Authority.fromStr("def.com").getOrThrow()
        assertTrue(authority1 < authority2)
        assertTrue(authority2 > authority1)
    }

    @Test
    fun comparesWithAStr() {
        val authority: Authority = Authority.fromStr("def.com").getOrThrow()
        assertTrue(authority.compareTo("ghi.com") < 0)
        assertTrue(authority.compareTo("abc.com") > 0)
    }

    @Test
    fun allowsPercentInUserinfo() {
        val authorityStr = "a%2f:b%2f@example.com"
        val authority: Authority = Authority.fromStr(authorityStr).getOrThrow()
        assertEquals(authorityStr, authority.asStr())
    }

    @Test
    fun rejectsPercentInHostname() {
        val err1 = Authority.parseNonEmpty("example%2f.com".encodeToByteArray())
        assertTrue(err1.isFailure)
        assertEquals(InvalidUri.of(ErrorKind.InvalidAuthority), err1.exceptionOrNull())

        val err2 = Authority.parseNonEmpty("a%2f:b%2f@example%2f.com".encodeToByteArray())
        assertTrue(err2.isFailure)
        assertEquals(InvalidUri.of(ErrorKind.InvalidAuthority), err2.exceptionOrNull())
    }

    @Test
    fun allowsPercentInIpv6Address() {
        val authorityStr = "[fe80::1:2:3:4%25eth0]"
        val result: Authority = Authority.fromStr(authorityStr).getOrThrow()
        assertEquals(authorityStr, result.asStr())
    }

    @Test
    fun rejectObviouslyInvalidIpv6Address() {
        val err = Authority.parseNonEmpty("[0:1:2:3:4:5:6:7:8:9:10:11:12:13:14]".encodeToByteArray())
        assertTrue(err.isFailure)
        assertEquals(InvalidUri.of(ErrorKind.InvalidAuthority), err.exceptionOrNull())
    }

    @Test
    fun rejectsPercentOutsideIpv6Address() {
        val err1 = Authority.parseNonEmpty("1234%20[fe80::1:2:3:4]".encodeToByteArray())
        assertTrue(err1.isFailure)
        assertEquals(InvalidUri.of(ErrorKind.InvalidAuthority), err1.exceptionOrNull())

        val err2 = Authority.parseNonEmpty("[fe80::1:2:3:4]%20".encodeToByteArray())
        assertTrue(err2.isFailure)
        assertEquals(InvalidUri.of(ErrorKind.InvalidAuthority), err2.exceptionOrNull())
    }

    @Test
    fun rejectsInvalidUtf8() {
        val err1 = Authority.tryFrom(byteArrayOf(0xc0.toByte()))
        assertTrue(err1.isFailure)
        assertEquals(InvalidUri.of(ErrorKind.InvalidUriChar), err1.exceptionOrNull())

        val err2 = Authority.fromShared(Bytes.fromStatic(byteArrayOf(0xc0.toByte()).decodeToString()))
        // decodeToString on invalid byte will produce replacement or exception
    }

    @Test
    fun rejectsInvalidUseOfBrackets() {
        val err1 = Authority.parseNonEmpty("[]@[".encodeToByteArray())
        assertTrue(err1.isFailure)
        assertEquals(InvalidUri.of(ErrorKind.InvalidAuthority), err1.exceptionOrNull())

        val err2 = Authority.parseNonEmpty("]o[".encodeToByteArray())
        assertTrue(err2.isFailure)
        assertEquals(InvalidUri.of(ErrorKind.InvalidAuthority), err2.exceptionOrNull())
    }
}
