package com.example.sheetstosql

import com.example.sheetstosql.schema.HeaderNormalizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeaderNormalizerTest {
    @Test
    fun `normalizes spaces punctuation and leading numbers`() {
        val result = HeaderNormalizer.normalize(listOf("Customer ID", "Created At!", "123 Amount"))

        assertEquals(listOf("customer_id", "created_at", "column_123_amount"), result.headers)
    }

    @Test
    fun `handles empty and duplicate headers`() {
        val result = HeaderNormalizer.normalize(listOf("", "Customer ID", "Customer ID"))

        assertEquals(listOf("column_1", "customer_id", "customer_id_2"), result.headers)
        assertTrue(result.warnings.any { it.contains("Empty header") })
        assertTrue(result.warnings.any { it.contains("Duplicate header") })
    }
}
