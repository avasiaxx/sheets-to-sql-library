import io.github.avasiaxx.sheetstosql.SheetsToSql
import io.github.avasiaxx.sheetstosql.config.SheetsToSqlConfig
import io.github.avasiaxx.sheetstosql.google.GoogleSheetsConfig
import io.github.avasiaxx.sheetstosql.google.SheetsAccessPolicy
import io.github.avasiaxx.sheetstosql.sql.PostgresDialect

fun main() {
    val spreadsheetId = requireNotNull(System.getenv("SPREADSHEET_ID")) {
        "SPREADSHEET_ID is required"
    }
    val sheetName = System.getenv("SHEET_NAME") ?: "Sheet1"

    val sheetsToSql = SheetsToSql.create(
        SheetsToSqlConfig(
            google = GoogleSheetsConfig.fromApplicationDefaultCredentials(
                accessPolicy = SheetsAccessPolicy.allowOnly(
                    spreadsheetIds = setOf(spreadsheetId),
                    sheetNames = setOf(sheetName)
                )
            ),
            dialect = PostgresDialect
        )
    )

    val sheet = sheetsToSql.readSheet(spreadsheetId, sheetName)
    val review = sheetsToSql.reviewSchema(sheet, tableName = sheetName.lowercase())

    println(review)
}
