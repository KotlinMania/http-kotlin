package io.github.kotlinmania.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class ScalarCloneParityTest {
    private fun versions(): List<Version> = listOf(Version.HTTP_09, Version.HTTP_10, Version.HTTP_11, Version.HTTP_2, Version.HTTP_3)

    @Test
    fun allStatuses() {
        for (code in 100..999) {
            val original = StatusCode.fromU16(code).getOrThrow()
            val clone = original.clone()
            assertEquals(original, clone)
            assertNotSame(original, clone)
            assertEquals(code, clone.asU16())
            assertEquals(original.asStr(), clone.asStr())
            assertEquals(original.toString(), clone.toString())
            assertEquals(original.hashCode(), clone.hashCode())
        }
    }

    @Test
    fun statusFlagsAndReasons() {
        for (code in listOf(100, 199, 200, 299, 300, 399, 400, 499, 500, 599, 600, 999)) {
            val original = StatusCode.fromU16(code).getOrThrow()
            val clone = original.clone()
            assertEquals(original.isInformational(), clone.isInformational())
            assertEquals(original.isSuccess(), clone.isSuccess())
            assertEquals(original.isRedirection(), clone.isRedirection())
            assertEquals(original.isClientError(), clone.isClientError())
            assertEquals(original.isServerError(), clone.isServerError())
            assertEquals(original.canonicalReason(), clone.canonicalReason())
        }
    }

    @Test
    fun statusOrder() {
        for ((aCode, bCode) in listOf(100 to 999, 200 to 404, 599 to 600, 999 to 999)) {
            val a = StatusCode.fromU16(aCode).getOrThrow()
            val b = StatusCode.fromU16(bCode).getOrThrow()
            assertEquals(a.compareTo(b).compareTo(0), a.clone().compareTo(b.clone()).compareTo(0))
        }
    }

    @Test
    fun versionValues() {
        for (original in versions()) {
            val clone = original.clone()
            assertEquals(original, clone)
            assertNotSame(original, clone)
            assertEquals(original.toString(), clone.toString())
            assertEquals(original.hashCode(), clone.hashCode())
        }
    }

    @Test
    fun versionOrder() {
        for (a in versions()) {
            for (b in versions()) {
                assertEquals(a.compareTo(b).compareTo(0), a.clone().compareTo(b.clone()).compareTo(0))
            }
        }
    }

    @Test
    fun cloneChainsAndDefault() {
        val standard = StatusCode.OK.clone().clone()
        assertEquals(200, standard.asU16())
        val custom = StatusCode.fromU16(999).getOrThrow()
        val customClone = custom.clone().clone()
        assertEquals(999, customClone.asU16())
        val defaultVersion = Version.default()
        assertEquals(Version.HTTP_11, defaultVersion.clone().clone())
    }
}
