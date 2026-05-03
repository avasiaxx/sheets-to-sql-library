package io.github.avasiaxx.sheetstosql

import io.github.avasiaxx.sheetstosql.errors.AccessDeniedException
import io.github.avasiaxx.sheetstosql.google.SheetsAccessPolicy
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SheetsAccessPolicyTest {
    @Test
    fun `denies all access by default`() {
        val policy = SheetsAccessPolicy.denyAll()

        assertFailsWith<AccessDeniedException> {
            policy.requireSpreadsheetAllowed("any-spreadsheet")
        }
    }

    @Test
    fun `denies spreadsheet ids outside allowlist`() {
        val policy = SheetsAccessPolicy.allowOnly(spreadsheetIds = setOf("allowed"))

        assertFailsWith<AccessDeniedException> {
            policy.requireSpreadsheetAllowed("denied")
        }
    }

    @Test
    fun `denies sheet names outside allowlist`() {
        val policy = SheetsAccessPolicy.allowOnly(
            spreadsheetIds = setOf("spreadsheet"),
            sheetNames = setOf("Customers")
        )

        assertFailsWith<AccessDeniedException> {
            policy.requireSheetAllowed("Payroll")
        }
    }

    @Test
    fun `denies ranges outside allowed patterns`() {
        val policy = SheetsAccessPolicy.allowOnly(
            spreadsheetIds = setOf("spreadsheet"),
            rangePatterns = listOf(Regex("'Customers'![A-Z]+\\d+:[A-Z]+\\d+"))
        )

        assertFailsWith<AccessDeniedException> {
            policy.requireRangeAllowed("'Payroll'!A1:Z100")
        }
    }

    @Test
    fun `can explicitly allow all for trusted local use`() {
        val policy = SheetsAccessPolicy.allowAllForTrustedLocalUseOnly()

        policy.requireSpreadsheetAllowed("any-spreadsheet")
        policy.requireSheetAllowed("any-sheet")
        policy.requireRangeAllowed("any-range")
    }
}
