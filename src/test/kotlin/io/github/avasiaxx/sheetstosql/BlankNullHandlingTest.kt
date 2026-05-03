package io.github.avasiaxx.sheetstosql

import io.github.avasiaxx.sheetstosql.schema.BlankValuePolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BlankNullHandlingTest {
    @Test
    fun `converts blank tokens to null by default`() {
        val policy = BlankValuePolicy.default()

        assertNull(policy.normalize(""))
        assertNull(policy.normalize(" N/A "))
        assertEquals("hello", policy.normalize("hello"))
    }

    @Test
    fun `can preserve empty strings`() {
        val policy = BlankValuePolicy(blankAsNull = false)

        assertEquals("", policy.normalize(""))
    }
}
