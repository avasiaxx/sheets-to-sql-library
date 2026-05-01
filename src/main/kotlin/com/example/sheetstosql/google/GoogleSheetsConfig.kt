package com.example.sheetstosql.google

import java.nio.file.Path

data class GoogleSheetsConfig(
    val credentialsPath: Path? = null,
    val applicationName: String = "sheets-to-sql",
    val accessPolicy: SheetsAccessPolicy = SheetsAccessPolicy.allowAll()
) {
    companion object {
        fun fromApplicationDefaultCredentials(
            applicationName: String = "sheets-to-sql",
            accessPolicy: SheetsAccessPolicy = SheetsAccessPolicy.allowAll()
        ): GoogleSheetsConfig = GoogleSheetsConfig(
            applicationName = applicationName,
            accessPolicy = accessPolicy
        )

        fun fromCredentialsFile(
            path: Path,
            applicationName: String = "sheets-to-sql",
            accessPolicy: SheetsAccessPolicy = SheetsAccessPolicy.allowAll()
        ): GoogleSheetsConfig = GoogleSheetsConfig(
            credentialsPath = path,
            applicationName = applicationName,
            accessPolicy = accessPolicy
        )
    }
}
