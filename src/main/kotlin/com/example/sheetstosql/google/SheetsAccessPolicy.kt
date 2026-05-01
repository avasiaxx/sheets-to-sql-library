package com.example.sheetstosql.google

import com.example.sheetstosql.errors.AccessDeniedException

data class SheetsAccessPolicy(
    val allowedSpreadsheetIds: Set<String> = emptySet(),
    val allowedSheetNames: Set<String> = emptySet(),
    val allowedRangePatterns: List<Regex> = emptyList()
) {
    fun requireSpreadsheetAllowed(spreadsheetId: String) {
        if (allowedSpreadsheetIds.isNotEmpty() && spreadsheetId !in allowedSpreadsheetIds) {
            throw AccessDeniedException("Spreadsheet access denied by policy")
        }
    }

    fun requireSheetAllowed(sheetName: String) {
        if (allowedSheetNames.isNotEmpty() && sheetName !in allowedSheetNames) {
            throw AccessDeniedException("Sheet access denied by policy")
        }
    }

    fun requireRangeAllowed(range: String) {
        if (allowedRangePatterns.isNotEmpty() && allowedRangePatterns.none { it.matches(range) }) {
            throw AccessDeniedException("Range access denied by policy")
        }
    }

    companion object {
        fun allowAll(): SheetsAccessPolicy = SheetsAccessPolicy()
    }
}
