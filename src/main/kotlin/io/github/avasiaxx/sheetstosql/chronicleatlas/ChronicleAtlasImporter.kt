package io.github.avasiaxx.sheetstosql.chronicleatlas

import io.github.avasiaxx.sheetstosql.SheetsToSql
import io.github.avasiaxx.sheetstosql.model.SheetData
import io.github.avasiaxx.sheetstosql.schema.HeaderKeys
import io.github.avasiaxx.sheetstosql.schema.HeaderNormalizer
import io.github.avasiaxx.sheetstosql.sql.SqliteDialect
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.time.Instant

class ChronicleAtlasImporter(
    private val readSheet: (spreadsheetId: String, sheetName: String) -> SheetData,
    private val config: ChronicleAtlasImportConfig = ChronicleAtlasImportConfig()
) {
    constructor(
        sheetsToSql: SheetsToSql,
        config: ChronicleAtlasImportConfig = ChronicleAtlasImportConfig()
    ) : this(sheetsToSql::readSheet, config)

    fun importSpreadsheet(spreadsheetId: String, databasePath: Path): ChronicleAtlasImportReport {
        DriverManager.getConnection("jdbc:sqlite:${databasePath.toAbsolutePath()}").use { connection ->
            return importSpreadsheet(spreadsheetId, connection, databasePath)
        }
    }

    fun importSpreadsheet(
        spreadsheetId: String,
        connection: Connection,
        databasePath: Path? = null
    ): ChronicleAtlasImportReport {
        val sheets = readSheets(spreadsheetId)
        val originalAutoCommit = connection.autoCommit
        connection.autoCommit = false

        return try {
            val reports = sheets.map { sheet -> importSheet(connection, sheet) }
            connection.commit()
            ChronicleAtlasImportReport(
                spreadsheetId = spreadsheetId,
                databasePath = databasePath,
                tables = reports
            )
        } catch (exception: Exception) {
            connection.rollback()
            throw exception
        } finally {
            connection.autoCommit = originalAutoCommit
        }
    }

    private fun readSheets(spreadsheetId: String): List<ChronicleAtlasSourceSheet> {
        return config.targets.map { target ->
            try {
                readSheet(spreadsheetId, target.sheetName).toChronicleAtlasSheet(target)
            } catch (exception: Exception) {
                ChronicleAtlasSourceSheet(
                    target = target,
                    headers = emptyList(),
                    rows = emptyList(),
                    warnings = listOf(
                        ChronicleAtlasImportWarning(
                            sheetName = target.sheetName,
                            tableName = target.tableName,
                            rowNumber = null,
                            message = "Configured sheet could not be read"
                        )
                    )
                )
            }
        }
    }

    private fun SheetData.toChronicleAtlasSheet(target: ChronicleAtlasSheetTarget): ChronicleAtlasSourceSheet {
        val expectedHeaderIndex = config.headerRowNumber - 1
        val warnings = mutableListOf<ChronicleAtlasImportWarning>()
        if (headerRowIndex != expectedHeaderIndex) {
            warnings += ChronicleAtlasImportWarning(
                sheetName = target.sheetName,
                tableName = target.tableName,
                rowNumber = config.headerRowNumber,
                message = "Detected header row did not match configured Chronicle Atlas layout"
            )
        }

        val rows = rows.mapIndexedNotNull { index, row ->
            val rowNumber = headerRowIndex + 2 + index
            if (rowNumber < config.dataStartRowNumber || row.values.all { it.isNullOrBlank() }) {
                null
            } else {
                ChronicleAtlasSourceRow(
                    rowNumber = rowNumber,
                    valuesByHeader = row.mapValues { (_, value) -> value?.trim()?.takeIf(String::isNotBlank) }
                )
            }
        }

        return ChronicleAtlasSourceSheet(
            target = target,
            headers = headers,
            rows = rows,
            warnings = warnings
        )
    }

    private fun importSheet(connection: Connection, sheet: ChronicleAtlasSourceSheet): ChronicleAtlasTableImportReport {
        val warnings = sheet.warnings.toMutableList()
        if (sheet.headers.isEmpty()) {
            return ChronicleAtlasTableImportReport(
                sheetName = sheet.target.sheetName,
                tableName = sheet.target.tableName,
                createdRows = 0,
                updatedRows = 0,
                skippedRows = 0,
                warningRows = warnings.mapNotNull { it.rowNumber }.distinct().size,
                warnings = warnings
            )
        }

        val columns = ChronicleAtlasTableColumns.from(sheet.headers)
        warnings += columns.warnings.map {
            ChronicleAtlasImportWarning(
                sheetName = sheet.target.sheetName,
                tableName = sheet.target.tableName,
                rowNumber = null,
                message = it
            )
        }

        SqliteDialect.validateIdentifier(sheet.target.tableName, "table name")
        createOrUpdateTable(connection, sheet.target.tableName, columns)

        var created = 0
        var updated = 0
        var skipped = 0

        sheet.rows.forEach { row ->
            val rowHash = rowHash(sheet.headers, row.valuesByHeader)
            val entityKey = entityKey(sheet.target, sheet.headers, row.valuesByHeader) ?: rowHash.also {
                warnings += ChronicleAtlasImportWarning(
                    sheetName = sheet.target.sheetName,
                    tableName = sheet.target.tableName,
                    rowNumber = row.rowNumber,
                    message = "Row has no configured entity key; row hash was used for repeatable imports"
                )
            }

            when (upsertRow(connection, sheet.target, columns, row, rowHash, entityKey)) {
                RowImportAction.CREATED -> created += 1
                RowImportAction.UPDATED -> updated += 1
                RowImportAction.SKIPPED -> skipped += 1
            }
        }

        return ChronicleAtlasTableImportReport(
            sheetName = sheet.target.sheetName,
            tableName = sheet.target.tableName,
            createdRows = created,
            updatedRows = updated,
            skippedRows = skipped,
            warningRows = warnings.mapNotNull { it.rowNumber }.distinct().size,
            warnings = warnings
        )
    }

    private fun createOrUpdateTable(
        connection: Connection,
        tableName: String,
        columns: ChronicleAtlasTableColumns
    ) {
        val definitions = (metadataColumns + columns.dataColumns).joinToString(",\n") { column ->
            val required = if (column in requiredMetadataColumns) " NOT NULL" else ""
            "  ${SqliteDialect.quoteIdentifier(column)} TEXT$required"
        }

        connection.createStatement().use { statement ->
            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS ${SqliteDialect.quoteIdentifier(tableName)} (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                $definitions,
                  UNIQUE(${SqliteDialect.quoteIdentifier(sourceEntityKeyColumn)})
                )
                """.trimIndent()
            )
        }

        val existingColumns = existingColumns(connection, tableName)
        columns.dataColumns
            .filterNot { it in existingColumns }
            .forEach { column ->
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        "ALTER TABLE ${SqliteDialect.quoteIdentifier(tableName)} ADD COLUMN ${SqliteDialect.quoteIdentifier(column)} TEXT"
                    )
                }
            }
    }

    private fun upsertRow(
        connection: Connection,
        target: ChronicleAtlasSheetTarget,
        columns: ChronicleAtlasTableColumns,
        row: ChronicleAtlasSourceRow,
        rowHash: String,
        entityKey: String
    ): RowImportAction {
        val existing = findExistingRow(connection, target.tableName, entityKey)
        val importedAt = Instant.now().toString()
        val valuesByColumn = columns.dataColumns.associateWith { column ->
            row.valuesByHeader[columns.headerByColumn.getValue(column)]
        }

        if (existing?.rowHash == rowHash && existing.sourceRowNumber == row.rowNumber) {
            return RowImportAction.SKIPPED
        }

        return if (existing == null) {
            insertRow(connection, target, columns, row, rowHash, entityKey, importedAt, valuesByColumn)
            RowImportAction.CREATED
        } else {
            updateRow(connection, target, columns, row, rowHash, entityKey, importedAt, valuesByColumn)
            RowImportAction.UPDATED
        }
    }

    private fun insertRow(
        connection: Connection,
        target: ChronicleAtlasSheetTarget,
        columns: ChronicleAtlasTableColumns,
        row: ChronicleAtlasSourceRow,
        rowHash: String,
        entityKey: String,
        importedAt: String,
        valuesByColumn: Map<String, String?>
    ) {
        val insertColumns = metadataColumns + columns.dataColumns
        val sql = """
            INSERT INTO ${SqliteDialect.quoteIdentifier(target.tableName)}
            (${insertColumns.joinToString(", ") { SqliteDialect.quoteIdentifier(it) }})
            VALUES (${insertColumns.joinToString(", ") { "?" }})
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            bindRow(statement, target, row, rowHash, entityKey, importedAt, insertColumns, valuesByColumn)
            statement.executeUpdate()
        }
    }

    private fun updateRow(
        connection: Connection,
        target: ChronicleAtlasSheetTarget,
        columns: ChronicleAtlasTableColumns,
        row: ChronicleAtlasSourceRow,
        rowHash: String,
        entityKey: String,
        importedAt: String,
        valuesByColumn: Map<String, String?>
    ) {
        val updateColumns = metadataColumns.filterNot { it == sourceEntityKeyColumn } + columns.dataColumns
        val sql = """
            UPDATE ${SqliteDialect.quoteIdentifier(target.tableName)}
            SET ${updateColumns.joinToString(", ") { "${SqliteDialect.quoteIdentifier(it)} = ?" }}
            WHERE ${SqliteDialect.quoteIdentifier(sourceEntityKeyColumn)} = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            bindRow(statement, target, row, rowHash, entityKey, importedAt, updateColumns, valuesByColumn)
            statement.setString(updateColumns.size + 1, entityKey)
            statement.executeUpdate()
        }
    }

    private fun bindRow(
        statement: PreparedStatement,
        target: ChronicleAtlasSheetTarget,
        row: ChronicleAtlasSourceRow,
        rowHash: String,
        entityKey: String,
        importedAt: String,
        columns: List<String>,
        valuesByColumn: Map<String, String?>
    ) {
        columns.forEachIndexed { index, column ->
            val value = when (column) {
                sourceSheetColumn -> target.sheetName
                sourceRowNumberColumn -> row.rowNumber.toString()
                sourceRowHashColumn -> rowHash
                sourceEntityKeyColumn -> entityKey
                importedAtColumn -> importedAt
                else -> valuesByColumn[column]
            }
            statement.setString(index + 1, value)
        }
    }

    private fun findExistingRow(connection: Connection, tableName: String, entityKey: String): ExistingRow? {
        val sql = """
            SELECT ${SqliteDialect.quoteIdentifier(sourceRowHashColumn)}, ${SqliteDialect.quoteIdentifier(sourceRowNumberColumn)}
            FROM ${SqliteDialect.quoteIdentifier(tableName)}
            WHERE ${SqliteDialect.quoteIdentifier(sourceEntityKeyColumn)} = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, entityKey)
            statement.executeQuery().use { result ->
                if (!result.next()) return null
                return ExistingRow(
                    rowHash = result.getString(1),
                    sourceRowNumber = result.getString(2).toIntOrNull()
                )
            }
        }
    }

    private fun existingColumns(connection: Connection, tableName: String): Set<String> {
        connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA table_info(${SqliteDialect.quoteIdentifier(tableName)})").use { result ->
                val columns = mutableSetOf<String>()
                while (result.next()) {
                    columns += result.getString("name")
                }
                return columns
            }
        }
    }

    private fun entityKey(
        target: ChronicleAtlasSheetTarget,
        headers: List<String>,
        row: Map<String, String?>
    ): String? {
        val candidates = target.entityKeyHeaders.mapNotNull { keyHeader ->
            headers.firstOrNull { it.equals(keyHeader, ignoreCase = true) }
        }
        return candidates
            .mapNotNull { row[it]?.trim()?.takeIf(String::isNotBlank) }
            .firstOrNull()
    }

    private fun rowHash(headers: List<String>, row: Map<String, String?>): String {
        val canonical = headers.mapIndexed { index, header ->
            val key = HeaderKeys.stable(headers, header, index)
            "${key}\u001E${row[key].orEmpty()}"
        }.joinToString("\u001F")
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private data class ExistingRow(
        val rowHash: String,
        val sourceRowNumber: Int?
    )

    private enum class RowImportAction {
        CREATED,
        UPDATED,
        SKIPPED
    }

    private data class ChronicleAtlasTableColumns(
        val headerByColumn: Map<String, String>,
        val dataColumns: List<String>,
        val warnings: List<String>
    ) {
        companion object {
            fun from(headers: List<String>): ChronicleAtlasTableColumns {
                val normalized = HeaderNormalizer.normalize(
                    headers = headers,
                    reservedIdentifiers = metadataColumns.toSet()
                )
                val columnByHeader = linkedMapOf<String, String>()
                val headerByColumn = linkedMapOf<String, String>()

                headers.forEachIndexed { index, header ->
                    val column = normalized.headers[index]
                    val key = HeaderKeys.stable(headers, header, index)
                    columnByHeader[key] = column
                    headerByColumn[column] = key
                }

                return ChronicleAtlasTableColumns(
                    headerByColumn = headerByColumn,
                    dataColumns = columnByHeader.values.toList(),
                    warnings = normalized.warnings
                )
            }
        }
    }

    private companion object {
        private const val sourceSheetColumn = "_source_sheet"
        private const val sourceRowNumberColumn = "_source_row_number"
        private const val sourceRowHashColumn = "_source_row_hash"
        private const val sourceEntityKeyColumn = "_source_entity_key"
        private const val importedAtColumn = "_imported_at"

        private val metadataColumns = listOf(
            sourceSheetColumn,
            sourceRowNumberColumn,
            sourceRowHashColumn,
            sourceEntityKeyColumn,
            importedAtColumn
        )
        private val requiredMetadataColumns = metadataColumns.toSet()
    }
}
