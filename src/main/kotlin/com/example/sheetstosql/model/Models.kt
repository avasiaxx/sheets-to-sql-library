package com.example.sheetstosql.model

enum class SqlType {
    TEXT,
    INTEGER,
    DECIMAL,
    BOOLEAN,
    DATE,
    TIMESTAMP
}

data class ColumnSchema(
    val originalHeader: String,
    val name: String,
    val type: SqlType,
    val nullable: Boolean,
    val warnings: List<String> = emptyList(),
    val sourceHeaderKey: String = originalHeader
)

data class TableSchema(
    val tableName: String,
    val columns: List<ColumnSchema>,
    val primaryKey: List<String> = emptyList()
)

data class SheetData(
    val spreadsheetId: String,
    val sheetName: String,
    val headers: List<String>,
    val rows: List<Map<String, String?>>,
    val headerRowIndex: Int = 0
)

data class SchemaReviewReport(
    val schema: TableSchema,
    val rowCount: Int,
    val headerRowIndex: Int,
    val warnings: List<String>,
    val errors: List<String>
)

data class SqlStatement(
    val sql: String,
    val parameters: List<Any?> = emptyList()
)
