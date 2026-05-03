package io.github.avasiaxx.sheetstosql.google

import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import java.nio.file.Files

object GoogleAuth {
    private val scopes = listOf("https://www.googleapis.com/auth/spreadsheets.readonly")

    fun credentialsAdapter(config: GoogleSheetsConfig): HttpCredentialsAdapter {
        val credentials = if (config.credentialsPath != null) {
            Files.newInputStream(config.credentialsPath).use { input ->
                GoogleCredentials.fromStream(input)
            }
        } else {
            GoogleCredentials.getApplicationDefault()
        }.createScoped(scopes)

        return HttpCredentialsAdapter(credentials)
    }
}
