package io.github.avasiaxx.sheetstosql.sql

import io.github.avasiaxx.sheetstosql.errors.SchemaException
import io.github.avasiaxx.sheetstosql.model.ColumnSchema
import io.github.avasiaxx.sheetstosql.model.SqlStatement
import io.github.avasiaxx.sheetstosql.model.SqlType
import io.github.avasiaxx.sheetstosql.model.TableSchema

object SqliteDialect : SqlDialect {
    override val name: String = "sqlite"

    override fun quoteIdentifier(identifier: String): String {
        validateIdentifier(identifier, "identifier")
        return "\"${identifier.replace("\"", "\"\"")}\""
    }

    override fun placeholder(index: Int): String = "?"

    override fun mapType(type: SqlType, column: ColumnSchema): String = when (type) {
        SqlType.INTEGER -> "INTEGER"
        SqlType.DECIMAL -> "REAL"
        else -> "TEXT"
    }

    override fun createTable(schema: TableSchema): SqlStatement {
        validateSchema(schema)

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
        validateSchema(schema)

        if (rows.isEmpty()) {
            return SqlStatement("-- No rows to insert for ${quoteIdentifier(schema.tableName)}")
        }

        val columns = schema.columns.map { it.name }
        val parameters = mutableListOf<Any?>()
        val valuesSql = rows.joinToString(",\n") { row ->
            val placeholders = columns.map { column ->
                parameters += row[column]
                placeholder(parameters.size)
            }
            "  (${placeholders.joinToString(", ")})"
        }

        val columnSql = columns.joinToString(", ") { quoteIdentifier(it) }
        val sql = "INSERT INTO ${quoteIdentifier(schema.tableName)} ($columnSql)\nVALUES\n$valuesSql;"
        return SqlStatement(sql = sql, parameters = parameters)
    }

    fun validateSchema(schema: TableSchema) {
        validateIdentifier(schema.tableName, "table name")
        schema.columns.forEach { column ->
            validateIdentifier(column.name, "column name '${column.originalHeader}'")
        }
        val columnNames = schema.columns.map { it.name }.toSet()
        schema.primaryKey.forEach { key ->
            validateIdentifier(key, "primary key column")
            if (key !in columnNames) {
                throw SchemaException("Invalid SQLite primary key column: '$key' is not present in the schema")
            }
        }
    }

    fun validateIdentifier(identifier: String, label: String) {
        if (identifier.isBlank()) {
            throw SchemaException("Invalid SQLite $label: value cannot be blank")
        }
        if (identifier.any { it.isISOControl() }) {
            throw SchemaException("Invalid SQLite $label: value cannot contain control characters")
        }
    }
}
