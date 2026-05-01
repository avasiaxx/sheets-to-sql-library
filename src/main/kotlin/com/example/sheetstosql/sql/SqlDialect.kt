package com.example.sheetstosql.sql

import com.example.sheetstosql.model.ColumnSchema
import com.example.sheetstosql.model.SqlStatement
import com.example.sheetstosql.model.SqlType
import com.example.sheetstosql.model.TableSchema

interface SqlDialect {
    val name: String

    fun quoteIdentifier(identifier: String): String

    fun placeholder(index: Int): String

    fun mapType(type: SqlType, column: ColumnSchema): String

    fun createTable(schema: TableSchema): SqlStatement

    fun insert(schema: TableSchema, rows: List<Map<String, Any?>>): SqlStatement

    fun upsert(
        schema: TableSchema,
        rows: List<Map<String, Any?>>,
        conflictKeys: List<String>
    ): SqlStatement {
        throw UnsupportedOperationException("Upsert is not supported for $name")
    }
}
