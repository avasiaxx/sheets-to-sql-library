package io.github.avasiaxx.sheetstosql.google

import java.nio.file.Path

data class GoogleSheetsConfig(
    val credentialsPath: Path? = null,
    val applicationName: String = "sheets-to-sql",
    val accessPolicy: SheetsAccessPolicy = SheetsAccessPolicy.denyAll()
) {
    companion object {
        fun fromApplicationDefaultCredentials(
            applicationName: String = "sheets-to-sql",
            accessPolicy: SheetsAccessPolicy = SheetsAccessPolicy.denyAll()
        ): GoogleSheetsConfig = GoogleSheetsConfig(
            applicationName = applicationName,
            accessPolicy = accessPolicy
        )

        fun fromCredentialsFile(
            path: Path,
            applicationName: String = "sheets-to-sql",
            accessPolicy: SheetsAccessPolicy = SheetsAccessPolicy.denyAll()
        ): GoogleSheetsConfig = GoogleSheetsConfig(
            credentialsPath = path,
            applicationName = applicationName,
            accessPolicy = accessPolicy
        )
    }
}
