package io.github.avasiaxx.sheetstosql.google

import io.github.avasiaxx.sheetstosql.errors.AccessDeniedException

data class SheetsAccessPolicy(
    val allowedSpreadsheetIds: Set<String> = emptySet(),
    val allowedSheetNames: Set<String> = emptySet(),
    val allowedRangePatterns: List<Regex> = emptyList(),
    val unrestricted: Boolean = false
) {
    fun requireSpreadsheetAllowed(spreadsheetId: String) {
        if (unrestricted) return
        if (spreadsheetId !in allowedSpreadsheetIds) {
            throw AccessDeniedException("Spreadsheet access denied by policy")
        }
    }

    fun requireSheetAllowed(sheetName: String) {
        if (unrestricted) return
        if (sheetName !in allowedSheetNames) {
            throw AccessDeniedException("Sheet access denied by policy")
        }
    }

    fun requireRangeAllowed(range: String) {
        if (unrestricted) return
        if (allowedRangePatterns.none { it.matches(range) }) {
            throw AccessDeniedException("Range access denied by policy")
        }
    }

    companion object {
        fun denyAll(): SheetsAccessPolicy = SheetsAccessPolicy()

        fun allowOnly(
            spreadsheetIds: Set<String>,
            sheetNames: Set<String> = emptySet(),
            rangePatterns: List<Regex> = emptyList()
        ): SheetsAccessPolicy = SheetsAccessPolicy(
            allowedSpreadsheetIds = spreadsheetIds,
            allowedSheetNames = sheetNames,
            allowedRangePatterns = rangePatterns
        )

        fun allowAllForTrustedLocalUseOnly(): SheetsAccessPolicy = SheetsAccessPolicy(unrestricted = true)
    }
}
