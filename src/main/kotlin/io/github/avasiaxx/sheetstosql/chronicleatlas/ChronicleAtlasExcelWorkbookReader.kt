package io.github.avasiaxx.sheetstosql.chronicleatlas

import io.github.avasiaxx.sheetstosql.schema.HeaderKeys
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.nio.file.Path

internal object ChronicleAtlasExcelWorkbookReader {
    fun read(workbookPath: Path, config: ChronicleAtlasImportConfig): List<ChronicleAtlasWorkbookSheet> {
        WorkbookFactory.create(workbookPath.toFile()).use { workbook ->
            val formatter = DataFormatter()
            val evaluator = workbook.creationHelper.createFormulaEvaluator()

            return config.targets.map { target ->
                val sheet = workbook.getSheet(target.sheetName)
                if (sheet == null) {
                    ChronicleAtlasWorkbookSheet(
                        target = target,
                        headers = emptyList(),
                        rows = emptyList(),
                        warnings = listOf(
                            ChronicleAtlasImportWarning(
                                sheetName = target.sheetName,
                                tableName = target.tableName,
                                rowNumber = null,
                                message = "Configured sheet was not found"
                            )
                        )
                    )
                } else {
                    val headerRow = sheet.getRow(config.headerRowNumber - 1)
                    val headers = readHeaders(headerRow)
                    val rows = ((config.dataStartRowNumber - 1)..sheet.lastRowNum)
                        .asSequence()
                        .mapNotNull { rowIndex ->
                            val row = sheet.getRow(rowIndex) ?: return@mapNotNull null
                            readDataRow(row, headers) { cell ->
                                formatter.formatCellValue(cell, evaluator).trim()
                            }
                        }
                        .toList()

                    val warnings = mutableListOf<ChronicleAtlasImportWarning>()
                    if (headers.isEmpty()) {
                        warnings += ChronicleAtlasImportWarning(
                            sheetName = target.sheetName,
                            tableName = target.tableName,
                            rowNumber = config.headerRowNumber,
                            message = "Header row is empty"
                        )
                    }

                    ChronicleAtlasWorkbookSheet(
                        target = target,
                        headers = headers,
                        rows = rows,
                        warnings = warnings
                    )
                }
            }
        }
    }

    private fun readHeaders(row: Row?): List<String> {
        if (row == null) return emptyList()
        return (0 until row.lastCellNum.coerceAtLeast(0))
            .map { index -> row.getCell(index)?.stringValue()?.trim().orEmpty() }
            .dropLastWhile { it.isBlank() }
    }

    private fun readDataRow(
        row: Row,
        headers: List<String>,
        formatCell: (Cell) -> String
    ): ChronicleAtlasWorkbookRow? {
        val values = headers.mapIndexed { index, header ->
            val raw = row.getCell(index)?.let(formatCell)
            HeaderKeys.stable(headers, header, index) to raw.takeUnless { it.isNullOrBlank() }
        }.toMap()

        if (values.values.all { it.isNullOrBlank() }) return null

        return ChronicleAtlasWorkbookRow(
            rowNumber = row.rowNum + 1,
            valuesByHeader = values
        )
    }

    private fun Cell.stringValue(): String = when (cellType) {
        else -> DataFormatter().formatCellValue(this)
    }
}
