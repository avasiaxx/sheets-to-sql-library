package io.github.avasiaxx.sheetstosql

import io.github.avasiaxx.sheetstosql.config.SheetsToSqlConfig
import io.github.avasiaxx.sheetstosql.google.GoogleSheetsReader
import io.github.avasiaxx.sheetstosql.model.SchemaReviewReport
import io.github.avasiaxx.sheetstosql.model.SheetData
import io.github.avasiaxx.sheetstosql.model.SqlStatement
import io.github.avasiaxx.sheetstosql.model.TableSchema
import io.github.avasiaxx.sheetstosql.schema.SchemaReviewer

class SheetsToSql private constructor(
    private val config: SheetsToSqlConfig,
    private val reader: GoogleSheetsReader
) {
    fun spreadsheetTitle(spreadsheetId: String): String = reader.spreadsheetTitle(spreadsheetId)

    fun listSheets(spreadsheetId: String): List<String> = reader.listSheets(spreadsheetId)

    fun readRange(spreadsheetId: String, range: String): List<List<String?>> = reader.readRange(spreadsheetId, range)

    fun readSheet(spreadsheetId: String, sheetName: String): SheetData = reader.readSheet(spreadsheetId, sheetName)

    fun reviewSchema(sheet: SheetData, tableName: String): SchemaReviewReport {
        return SchemaReviewer.review(sheet, tableName, config.blankValuePolicy)
    }

    fun generateCreateTable(schema: TableSchema): SqlStatement = config.dialect.createTable(schema)

    fun generateInsert(schema: TableSchema, rows: List<Map<String, Any?>>): SqlStatement {
        return config.dialect.insert(schema, alignRowsToSchema(schema, rows))
    }

    fun generateUpsert(
        schema: TableSchema,
        rows: List<Map<String, Any?>>,
        conflictKeys: List<String>
    ): SqlStatement {
        return config.dialect.upsert(schema, alignRowsToSchema(schema, rows), conflictKeys)
    }

    private fun alignRowsToSchema(
        schema: TableSchema,
        rows: List<Map<String, Any?>>
    ): List<Map<String, Any?>> {
        return rows.map { row ->
            schema.columns.associate { column ->
                val value = when {
                    row.containsKey(column.name) -> row[column.name]
                    row.containsKey(column.sourceHeaderKey) -> row[column.sourceHeaderKey]
                    row.containsKey(column.originalHeader) -> row[column.originalHeader]
                    else -> null
                }
                column.name to value
            }
        }
    }

    companion object {
        fun create(config: SheetsToSqlConfig = SheetsToSqlConfig()): SheetsToSql {
            return SheetsToSql(
                config = config,
                reader = GoogleSheetsReader(config.google, config.blankValuePolicy)
            )
        }
    }
}
