package com.example.sheetstosql.sql

import com.example.sheetstosql.model.ColumnSchema
import com.example.sheetstosql.model.SqlStatement
import com.example.sheetstosql.model.SqlType
import com.example.sheetstosql.model.TableSchema

object PostgresDialect : SqlDialect {
    override val name: String = "postgres"

    override fun quoteIdentifier(identifier: String): String = "\"${identifier.replace("\"", "\"\"")}\""

    override fun placeholder(index: Int): String = "\$$index"

    override fun mapType(type: SqlType, column: ColumnSchema): String = when (type) {
        SqlType.TEXT -> "TEXT"
        SqlType.INTEGER -> "INTEGER"
        SqlType.DECIMAL -> "NUMERIC"
        SqlType.BOOLEAN -> "BOOLEAN"
        SqlType.DATE -> "DATE"
        SqlType.TIMESTAMP -> "TIMESTAMPTZ"
    }

    override fun createTable(schema: TableSchema): SqlStatement {
        val columnLines = schema.columns.map { column ->
            val nullable = if (column.nullable) "" else " NOT NULL"
            "  ${quoteIdentifier(column.name)} ${mapType(column.type, column)}$nullable"
        }

        val primaryKeyLine = if (schema.primaryKey.isNotEmpty()) {
            val keys = schema.primaryKey.joinToString(", ") { quoteIdentifier(it) }
            listOf("  PRIMARY KEY ($keys)")
        } else {
            emptyList()
        }

        val body = (columnLines + primaryKeyLine).joinToString(",\n")
        return SqlStatement(
            sql = "CREATE TABLE IF NOT EXISTS ${quoteIdentifier(schema.tableName)} (\n$body\n);"
        )
    }

    override fun insert(schema: TableSchema, rows: List<Map<String, Any?>>): SqlStatement {
        if (rows.isEmpty()) {
            return SqlStatement("-- No rows to insert for ${quoteIdentifier(schema.tableName)}")
        }

        val columns = schema.columns.map { it.name }
        val parameters = mutableListOf<Any?>()
        var parameterIndex = 1

        val valuesSql = rows.joinToString(",\n") { row ->
            val placeholders = columns.map { column ->
                parameters += row[column]
                placeholder(parameterIndex++)
            }
            "  (${placeholders.joinToString(", ")})"
        }

        val columnSql = columns.joinToString(", ") { quoteIdentifier(it) }
        val sql = "INSERT INTO ${quoteIdentifier(schema.tableName)} ($columnSql)\nVALUES\n$valuesSql;"
        return SqlStatement(sql = sql, parameters = parameters)
    }

    override fun upsert(
        schema: TableSchema,
        rows: List<Map<String, Any?>>,
        conflictKeys: List<String>
    ): SqlStatement {
        if (conflictKeys.isEmpty()) {
            throw IllegalArgumentException("PostgreSQL upsert requires at least one conflict key")
        }

        val insert = insert(schema, rows)
        if (rows.isEmpty()) return insert

        val conflictSql = conflictKeys.joinToString(", ") { quoteIdentifier(it) }
        val updateColumns = schema.columns.map { it.name }.filterNot { it in conflictKeys }

        val sql = if (updateColumns.isEmpty()) {
            insert.sql.removeSuffix(";") + "\nON CONFLICT ($conflictSql) DO NOTHING;"
        } else {
            val assignments = updateColumns.joinToString(", ") {
                "${quoteIdentifier(it)} = EXCLUDED.${quoteIdentifier(it)}"
            }
            insert.sql.removeSuffix(";") + "\nON CONFLICT ($conflictSql) DO UPDATE SET $assignments;"
        }

        return insert.copy(sql = sql)
    }
}
