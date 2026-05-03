package io.github.avasiaxx.sheetstosql

import io.github.avasiaxx.sheetstosql.chronicleatlas.ChronicleAtlasImporter
import io.github.avasiaxx.sheetstosql.model.SheetData
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChronicleAtlasImporterTest {
    @Test
    fun `imports characters and npcs from Google sheet data with repeatable SQLite upserts`() {
        val tempDir = kotlin.io.path.createTempDirectory("chronicle-atlas-import")
        val databasePath = tempDir.resolve("campaign.db")
        val spreadsheetId = "approved-spreadsheet-id"

        var characterClass = "Wizard"
        val importer = ChronicleAtlasImporter(readSheet = { requestedSpreadsheetId: String, sheetName: String ->
            assertEquals(spreadsheetId, requestedSpreadsheetId)
            sheetData(sheetName, characterClass = characterClass, npcLocation = "Library")
        })

        val firstReport = importer.importSpreadsheet(spreadsheetId, databasePath)

        assertEquals(2, firstReport.createdRows)
        assertEquals(0, firstReport.updatedRows)
        assertEquals(0, firstReport.skippedRows)
        assertEquals(0, firstReport.warningRows)

        val secondReport = importer.importSpreadsheet(spreadsheetId, databasePath)

        assertEquals(0, secondReport.createdRows)
        assertEquals(0, secondReport.updatedRows)
        assertEquals(2, secondReport.skippedRows)

        characterClass = "Cleric"
        val thirdReport = importer.importSpreadsheet(spreadsheetId, databasePath)

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

    private fun sheetData(
        sheetName: String,
        characterClass: String,
        npcLocation: String
    ): SheetData {
        return when (sheetName) {
            "Characters" -> SheetData(
                spreadsheetId = "approved-spreadsheet-id",
                sheetName = "Characters",
                headers = listOf("Name", "Class", "Notes"),
                rows = listOf(
                    mapOf("Name" to "Ada", "Class" to characterClass, "Notes" to "Sparse text"),
                    mapOf("Name" to null, "Class" to null, "Notes" to null)
                ),
                headerRowIndex = 1
            )
            "NPCs" -> SheetData(
                spreadsheetId = "approved-spreadsheet-id",
                sheetName = "NPCs",
                headers = listOf("Name", "Location", "Notes"),
                rows = listOf(
                    mapOf("Name" to "Borin", "Location" to npcLocation, "Notes" to null)
                ),
                headerRowIndex = 1
            )
            else -> error("Unexpected sheet requested")
        }
    }
}
