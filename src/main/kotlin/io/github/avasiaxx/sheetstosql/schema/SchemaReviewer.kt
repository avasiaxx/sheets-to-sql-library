package io.github.avasiaxx.sheetstosql.schema

import io.github.avasiaxx.sheetstosql.model.ColumnSchema
import io.github.avasiaxx.sheetstosql.model.SchemaReviewReport
import io.github.avasiaxx.sheetstosql.model.SheetData
import io.github.avasiaxx.sheetstosql.model.TableSchema

object SchemaReviewer {
    fun review(
        sheet: SheetData,
        tableName: String,
        blankValuePolicy: BlankValuePolicy = BlankValuePolicy.default()
    ): SchemaReviewReport {
        val normalized = HeaderNormalizer.normalize(sheet.headers)
        val warnings = normalized.warnings.toMutableList()

        val columns = sheet.headers.mapIndexed { index, originalHeader ->
            val normalizedName = normalized.headers[index]
            val stableOriginalHeader = HeaderKeys.stable(sheet.headers, originalHeader, index)
            val values = sheet.rows.map { row -> row[stableOriginalHeader] ?: row[originalHeader] }
            val type = TypeInferrer.infer(values, blankValuePolicy)
            val nullable = values.any { blankValuePolicy.normalize(it) == null }

            ColumnSchema(
                originalHeader = originalHeader,
                name = normalizedName,
                type = type,
                nullable = nullable,
                sourceHeaderKey = stableOriginalHeader
            )
        }

        val errors = mutableListOf<String>()
        if (tableName.isBlank()) errors += "Table name cannot be blank"
        if (sheet.headers.isEmpty()) errors += "No headers found"

        return SchemaReviewReport(
            schema = TableSchema(tableName = tableName, columns = columns),
            rowCount = sheet.rows.size,
            headerRowIndex = sheet.headerRowIndex,
            warnings = warnings,
            errors = errors
        )
    }
}
