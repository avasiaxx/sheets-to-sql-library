package io.github.avasiaxx.sheetstosql.sql

import io.github.avasiaxx.sheetstosql.errors.SchemaException
import io.github.avasiaxx.sheetstosql.model.TableSchema

object SqlIdentifierValidator {
    private const val POSTGRES_IDENTIFIER_LIMIT = 63

    fun validatePostgresSchema(schema: TableSchema) {
        validatePostgresIdentifier(schema.tableName, "table name")
        schema.columns.forEach { column ->
            validatePostgresIdentifier(column.name, "column name '${column.originalHeader}'")
        }
        val columnNames = schema.columns.map { it.name }.toSet()
        schema.primaryKey.forEach { key ->
            validatePostgresIdentifier(key, "primary key column")
            if (key !in columnNames) {
                throw SchemaException("Invalid SQL primary key column: '$key' is not present in the schema")
            }
        }
    }

    fun validatePostgresConflictKeys(schema: TableSchema, conflictKeys: List<String>) {
        val columnNames = schema.columns.map { it.name }.toSet()
        conflictKeys.forEach { key ->
            validatePostgresIdentifier(key, "conflict key")
            if (key !in columnNames) {
                throw SchemaException("Invalid SQL conflict key: '$key' is not present in the schema")
            }
        }
    }

    private fun validatePostgresIdentifier(identifier: String, label: String) {
        if (identifier.isBlank()) {
            throw SchemaException("Invalid SQL $label: value cannot be blank")
        }
        if (identifier.any { it.isISOControl() }) {
            throw SchemaException("Invalid SQL $label: value cannot contain control characters")
        }
        if (identifier.length > POSTGRES_IDENTIFIER_LIMIT) {
            throw SchemaException("Invalid SQL $label: value exceeds the conservative 63-character PostgreSQL identifier limit")
        }
    }
}
