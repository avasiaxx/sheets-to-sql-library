package com.example.sheetstosql.google

import com.example.sheetstosql.errors.SheetReadException
import com.example.sheetstosql.model.SheetData
import com.example.sheetstosql.schema.BlankValuePolicy
import com.example.sheetstosql.schema.HeaderRowDetector
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets

class GoogleSheetsReader(
    private val config: GoogleSheetsConfig,
    private val blankValuePolicy: BlankValuePolicy = BlankValuePolicy.default()
) {
    private val service: Sheets by lazy {
        Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            GoogleAuth.credentialsAdapter(config)
        )
            .setApplicationName(config.applicationName)
            .build()
    }

    fun spreadsheetTitle(spreadsheetId: String): String {
        config.accessPolicy.requireSpreadsheetAllowed(spreadsheetId)
        return try {
            service.spreadsheets().get(spreadsheetId)
                .setIncludeGridData(false)
                .execute()
                .properties
                .title
        } catch (exception: Exception) {
            throw SheetReadException("Unable to read spreadsheet metadata", exception)
        }
    }

    fun listSheets(spreadsheetId: String): List<String> {
        config.accessPolicy.requireSpreadsheetAllowed(spreadsheetId)
        return try {
            service.spreadsheets().get(spreadsheetId)
                .setIncludeGridData(false)
                .execute()
                .sheets
                .mapNotNull { it.properties?.title }
        } catch (exception: Exception) {
            throw SheetReadException("Unable to list sheets", exception)
        }
    }

    fun readRange(spreadsheetId: String, range: String): List<List<String?>> {
        config.accessPolicy.requireSpreadsheetAllowed(spreadsheetId)
        config.accessPolicy.requireRangeAllowed(range)
        return fetchRange(spreadsheetId, range)
    }

    fun readSheet(spreadsheetId: String, sheetName: String): SheetData {
        config.accessPolicy.requireSpreadsheetAllowed(spreadsheetId)
        config.accessPolicy.requireSheetAllowed(sheetName)
        val rows = fetchRange(spreadsheetId, "'${sheetName.replace("'", "''")}'")
        val headerRowIndex = HeaderRowDetector.detect(rows)
        val headers = rows.getOrNull(headerRowIndex).orEmpty().mapIndexed { index, value ->
            value ?: "column_${index + 1}"
        }
        val dataRows = rows.drop(headerRowIndex + 1).map { row ->
            headers.mapIndexed { index, header ->
                stableHeaderKey(headers, header, index) to row.getOrNull(index)
            }.toMap()
        }

        return SheetData(
            spreadsheetId = spreadsheetId,
            sheetName = sheetName,
            headers = headers,
            rows = dataRows,
            headerRowIndex = headerRowIndex
        )
    }

    private fun fetchRange(spreadsheetId: String, range: String): List<List<String?>> {
        return try {
            service.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute()
                .getValues()
                .orEmpty()
                .map { row -> row.map { blankValuePolicy.normalize(it?.toString()) } }
        } catch (exception: Exception) {
            throw SheetReadException("Unable to read spreadsheet range", exception)
        }
    }

    private fun stableHeaderKey(headers: List<String>, header: String, index: Int): String {
        val priorMatches = headers.take(index).count { it == header }
        return if (priorMatches == 0) header else "$header#${priorMatches + 1}"
    }
}
