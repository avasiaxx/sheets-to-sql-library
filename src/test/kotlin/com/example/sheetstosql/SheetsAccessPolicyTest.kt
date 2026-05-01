package com.example.sheetstosql

import com.example.sheetstosql.errors.AccessDeniedException
import com.example.sheetstosql.google.SheetsAccessPolicy
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SheetsAccessPolicyTest {
    @Test
    fun `denies spreadsheet ids outside allowlist`() {
        val policy = SheetsAccessPolicy(allowedSpreadsheetIds = setOf("allowed"))

        assertFailsWith<AccessDeniedException> {
            policy.requireSpreadsheetAllowed("denied")
        }
    }

    @Test
    fun `denies sheet names outside allowlist`() {
        val policy = SheetsAccessPolicy(allowedSheetNames = setOf("Customers"))

        assertFailsWith<AccessDeniedException> {
            policy.requireSheetAllowed("Payroll")
        }
    }

    @Test
    fun `denies ranges outside allowed patterns`() {
        val policy = SheetsAccessPolicy(
            allowedRangePatterns = listOf(Regex("'Customers'![A-Z]+\\d+:[A-Z]+\\d+"))
        )

        assertFailsWith<AccessDeniedException> {
            policy.requireRangeAllowed("'Payroll'!A1:Z100")
        }
    }
}
