// port-lint: source extensions.rs
package io.github.kotlinmania.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExtensionsTest {
    @Test
    fun testExtensions() {
        data class MyType(
            val value: Int,
        )

        val extensions = Extensions.new()
        assertEquals("{}", extensions.debugString())

        extensions.insert(5)
        extensions.insert(MyType(10))

        assertEquals(5, extensions.get<Int>())
        assertEquals(5, extensions.getMut<Int>())

        val dbg = extensions.debugString()
        // map order is NOT deterministic
        assertTrue(
            (dbg == "{MyType, Int}") ||
                (dbg == "{Int, MyType}"),
            dbg,
        )

        val ext2 = extensions.clone()

        assertEquals(5, extensions.remove<Int>())
        assertNull(extensions.get<Int>())

        // clone still has it
        assertEquals(5, ext2.get<Int>())
        assertEquals(MyType(10), ext2.get<MyType>())

        assertNull(extensions.get<Boolean>())
        assertEquals(MyType(10), extensions.get<MyType>())
    }
}
