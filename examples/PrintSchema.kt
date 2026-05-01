import com.example.sheetstosql.SheetsToSql
import com.example.sheetstosql.config.SheetsToSqlConfig
import com.example.sheetstosql.google.GoogleSheetsConfig
import com.example.sheetstosql.sql.PostgresDialect

fun main() {
    val spreadsheetId = requireNotNull(System.getenv("SPREADSHEET_ID")) {
        "SPREADSHEET_ID is required"
    }
    val sheetName = System.getenv("SHEET_NAME") ?: "Sheet1"

    val sheetsToSql = SheetsToSql.create(
        SheetsToSqlConfig(
            google = GoogleSheetsConfig.fromApplicationDefaultCredentials(),
            dialect = PostgresDialect
        )
    )

    val sheet = sheetsToSql.readSheet(spreadsheetId, sheetName)
    val review = sheetsToSql.reviewSchema(sheet, tableName = sheetName.lowercase())

    println(review)
}
