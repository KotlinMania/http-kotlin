// port-lint: source tests/status_code.rs
package io.github.kotlinmania.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatusCodeTest {
    @Test
    fun fromBytes() {
        for (ok in listOf("100", "101", "199", "200", "250", "299", "321", "399", "499", "599", "600", "999")) {
            assertTrue(StatusCode.fromBytes(ok.encodeToByteArray()).isSuccess)
        }

        for (notOk in listOf("0", "00", "10", "40", "99", "000", "010", "099", "1000", "1999")) {
            assertTrue(StatusCode.fromBytes(notOk.encodeToByteArray()).isFailure)
        }
    }

    @Test
    fun equatesWithU16() {
        val status = StatusCode.fromU16(200).getOrThrow()
        assertTrue(status.eq(200))
        assertEquals(200, status.asU16())
        assertFalse(status.equals(200))
    }

    @Test
    fun roundtrip() {
        for (s in 100 until 1000) {
            val sstr = s.toString()
            val status = StatusCode.fromBytes(sstr.encodeToByteArray()).getOrThrow()
            assertEquals(s, status.asU16())
            assertEquals(sstr, status.asStr())
            assertEquals(status, StatusCode.fromStr(sstr).getOrThrow())
            assertEquals(sstr, status.fmt())
        }
    }

    @Test
    fun isInformational() {
        assertTrue(statusCode(100).isInformational())
        assertTrue(statusCode(199).isInformational())

        assertFalse(statusCode(200).isInformational())
    }

    @Test
    fun isSuccess() {
        assertTrue(statusCode(200).isSuccess())
        assertTrue(statusCode(299).isSuccess())

        assertFalse(statusCode(199).isSuccess())
        assertFalse(statusCode(300).isSuccess())
    }

    @Test
    fun isRedirection() {
        assertTrue(statusCode(300).isRedirection())
        assertTrue(statusCode(399).isRedirection())

        assertFalse(statusCode(299).isRedirection())
        assertFalse(statusCode(400).isRedirection())
    }

    @Test
    fun isClientError() {
        assertTrue(statusCode(400).isClientError())
        assertTrue(statusCode(499).isClientError())

        assertFalse(statusCode(399).isClientError())
        assertFalse(statusCode(500).isClientError())
    }

    @Test
    fun isServerError() {
        assertTrue(statusCode(500).isServerError())
        assertTrue(statusCode(599).isServerError())

        assertFalse(statusCode(499).isServerError())
        assertFalse(statusCode(600).isServerError())
    }

    /** Helper method for readability */
    private fun statusCode(statusCode: Int): StatusCode = StatusCode.fromU16(statusCode).getOrThrow()
}
