package com.example.sheetstosql

import com.example.sheetstosql.model.ColumnSchema
import com.example.sheetstosql.model.SqlType
import com.example.sheetstosql.model.TableSchema
import com.example.sheetstosql.sql.PostgresDialect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlGenerationTest {
    private val schema = TableSchema(
        tableName = "customers",
        columns = listOf(
            ColumnSchema("Customer ID", "customer_id", SqlType.INTEGER, nullable = false),
            ColumnSchema("Email", "email", SqlType.TEXT, nullable = true)
        ),
        primaryKey = listOf("customer_id")
    )

    @Test
    fun `generates postgres create table`() {
        val statement = PostgresDialect.createTable(schema)

        assertTrue(statement.sql.contains("CREATE TABLE IF NOT EXISTS \"customers\""))
        assertTrue(statement.sql.contains("\"customer_id\" INTEGER NOT NULL"))
        assertTrue(statement.sql.contains("PRIMARY KEY (\"customer_id\")"))
    }

    @Test
    fun `generates parameterized insert`() {
        val statement = PostgresDialect.insert(
            schema,
            listOf(mapOf("customer_id" to 1, "email" to "a@example.com"))
        )

        assertTrue(statement.sql.contains("VALUES"))
        assertTrue(statement.sql.contains("\$1"))
        assertEquals(listOf(1, "a@example.com"), statement.parameters)
    }
}
