package io.github.avasiaxx.sheetstosql

import io.github.avasiaxx.sheetstosql.chronicleatlas.ChronicleAtlasImporter
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.nio.file.Path
import java.sql.DriverManager
import kotlin.io.path.outputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChronicleAtlasImporterTest {
    @Test
    fun `imports characters and npcs with repeatable SQLite upserts`() {
        val tempDir = kotlin.io.path.createTempDirectory("chronicle-atlas-import")
        val workbookPath = tempDir.resolve("campaign.xlsx")
        val databasePath = tempDir.resolve("campaign.db")

        writeWorkbook(workbookPath, characterClass = "Wizard", npcLocation = "Library")

        val importer = ChronicleAtlasImporter()
        val firstReport = importer.importWorkbook(workbookPath, databasePath)

        assertEquals(2, firstReport.createdRows)
        assertEquals(0, firstReport.updatedRows)
        assertEquals(0, firstReport.skippedRows)
        assertEquals(0, firstReport.warningRows)

        val secondReport = importer.importWorkbook(workbookPath, databasePath)

        assertEquals(0, secondReport.createdRows)
        assertEquals(0, secondReport.updatedRows)
        assertEquals(2, secondReport.skippedRows)

        writeWorkbook(workbookPath, characterClass = "Cleric", npcLocation = "Library")
        val thirdReport = importer.importWorkbook(workbookPath, databasePath)

        assertEquals(0, thirdReport.createdRows)
        assertEquals(1, thirdReport.updatedRows)
        assertEquals(1, thirdReport.skippedRows)

        DriverManager.getConnection("jdbc:sqlite:${databasePath.toAbsolutePath()}").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT COUNT(*) FROM chronicle_atlas_characters").use { result ->
                    assertTrue(result.next())
                    assertEquals(1, result.getInt(1))
                }
                statement.executeQuery("SELECT class, _source_sheet, _source_row_number, length(_source_row_hash) FROM chronicle_atlas_characters").use { result ->
                    assertTrue(result.next())
                    assertEquals("Cleric", result.getString("class"))
                    assertEquals("Characters", result.getString("_source_sheet"))
                    assertEquals("3", result.getString("_source_row_number"))
                    assertEquals(64, result.getInt(4))
                }
            }
        }
    }

    private fun writeWorkbook(workbookPath: Path, characterClass: String, npcLocation: String) {
        XSSFWorkbook().use { workbook ->
            workbook.addChronicleSheet(
                sheetName = "Characters",
                headers = listOf("Name", "Class", "Notes"),
                rows = listOf(
                    listOf("Ada", characterClass, "Sparse text"),
                    listOf("", "", "")
                )
            )
            workbook.addChronicleSheet(
                sheetName = "NPCs",
                headers = listOf("Name", "Location", "Notes"),
                rows = listOf(
                    listOf("Borin", npcLocation, "")
                )
            )

            workbookPath.outputStream().use { output -> workbook.write(output) }
        }
    }

    private fun Workbook.addChronicleSheet(
        sheetName: String,
        headers: List<String>,
        rows: List<List<String>>
    ) {
        val sheet = createSheet(sheetName)
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, headers.lastIndex))
        sheet.createRow(0).createCell(0).setCellValue("$sheetName Title")

        val headerRow = sheet.createRow(1)
        headers.forEachIndexed { index, header -> headerRow.createCell(index).setCellValue(header) }

        rows.forEachIndexed { rowIndex, values ->
            val row = sheet.createRow(rowIndex + 2)
            values.forEachIndexed { columnIndex, value -> row.createCell(columnIndex).setCellValue(value) }
        }

        repeat(200) { rowIndex ->
            val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
            repeat(headers.size) { columnIndex ->
                row.getCell(columnIndex) ?: row.createCell(columnIndex)
            }
        }
    }
}
