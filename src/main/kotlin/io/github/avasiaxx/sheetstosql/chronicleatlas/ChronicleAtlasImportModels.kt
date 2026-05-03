package io.github.avasiaxx.sheetstosql.chronicleatlas

import java.nio.file.Path

data class ChronicleAtlasImportConfig(
    val targets: List<ChronicleAtlasSheetTarget> = ChronicleAtlasSheetTarget.defaults,
    val headerRowNumber: Int = 2,
    val dataStartRowNumber: Int = 3
) {
    init {
        require(headerRowNumber >= 1) { "headerRowNumber must be 1 or greater" }
        require(dataStartRowNumber > headerRowNumber) { "dataStartRowNumber must be after headerRowNumber" }
        require(targets.isNotEmpty()) { "At least one import target is required" }
    }
}

data class ChronicleAtlasSheetTarget(
    val sheetName: String,
    val tableName: String,
    val entityKeyHeaders: List<String> = listOf("Name")
) {
    init {
        require(sheetName.isNotBlank()) { "sheetName cannot be blank" }
        require(tableName.isNotBlank()) { "tableName cannot be blank" }
    }

    companion object {
        val defaults: List<ChronicleAtlasSheetTarget> = listOf(
            ChronicleAtlasSheetTarget(sheetName = "Characters", tableName = "chronicle_atlas_characters"),
            ChronicleAtlasSheetTarget(sheetName = "NPCs", tableName = "chronicle_atlas_npcs")
        )
    }
}

data class ChronicleAtlasImportReport(
    val spreadsheetId: String,
    val databasePath: Path?,
    val tables: List<ChronicleAtlasTableImportReport>
) {
    val createdRows: Int get() = tables.sumOf { it.createdRows }
    val updatedRows: Int get() = tables.sumOf { it.updatedRows }
    val skippedRows: Int get() = tables.sumOf { it.skippedRows }
    val warningRows: Int get() = tables.sumOf { it.warningRows }
    val warnings: List<ChronicleAtlasImportWarning> get() = tables.flatMap { it.warnings }
}

data class ChronicleAtlasTableImportReport(
    val sheetName: String,
    val tableName: String,
    val createdRows: Int,
    val updatedRows: Int,
    val skippedRows: Int,
    val warningRows: Int,
    val warnings: List<ChronicleAtlasImportWarning>
)

data class ChronicleAtlasImportWarning(
    val sheetName: String,
    val tableName: String,
    val rowNumber: Int?,
    val message: String
)

internal data class ChronicleAtlasSourceSheet(
    val target: ChronicleAtlasSheetTarget,
    val headers: List<String>,
    val rows: List<ChronicleAtlasSourceRow>,
    val warnings: List<ChronicleAtlasImportWarning>
)

internal data class ChronicleAtlasSourceRow(
    val rowNumber: Int,
    val valuesByHeader: Map<String, String?>
)
