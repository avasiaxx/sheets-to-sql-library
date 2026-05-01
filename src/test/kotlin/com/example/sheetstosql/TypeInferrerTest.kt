package com.example.sheetstosql

import com.example.sheetstosql.model.SqlType
import com.example.sheetstosql.schema.TypeInferrer
import kotlin.test.Test
import kotlin.test.assertEquals

class TypeInferrerTest {
    @Test
    fun `infers primitive types`() {
        assertEquals(SqlType.INTEGER, TypeInferrer.infer(listOf("1", "2", null)))
        assertEquals(SqlType.DECIMAL, TypeInferrer.infer(listOf("1", "2.25")))
        assertEquals(SqlType.BOOLEAN, TypeInferrer.infer(listOf("true", "false", "yes")))
        assertEquals(SqlType.DATE, TypeInferrer.infer(listOf("2026-05-01", "2026-05-02")))
        assertEquals(SqlType.TIMESTAMP, TypeInferrer.infer(listOf("2026-05-01T12:30:00Z")))
    }

    @Test
    fun `falls back to text for mixed values`() {
        assertEquals(SqlType.TEXT, TypeInferrer.infer(listOf("1", "hello")))
    }
}
