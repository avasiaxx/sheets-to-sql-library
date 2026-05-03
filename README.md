# sheets-to-sql

Reusable Kotlin/JVM library for reading Google Sheets, reviewing inferred schemas, and generating SQL-ready table definitions and data operations.

The first implementation targets PostgreSQL SQL generation. The library is intentionally split into small layers:

- Google Sheets reader
- Header detection and normalization
- Type inference
- Schema review reports
- SQL dialect adapters

## Install

This project is currently scaffolded as a local Gradle library:

```kotlin
dependencies {
    implementation("io.github.avasiaxx:sheets-to-sql:0.1.0-SNAPSHOT")
}
```

The Kotlin package namespace is `io.github.avasiaxx.sheetstosql`.

## Use From Maven Local

Install the current snapshot into Maven Local:

```bash
./gradlew publishToMavenLocal
```

Then import it from another Gradle project:

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.github.avasiaxx:sheets-to-sql:0.1.0-SNAPSHOT")
}
```

## Using A Google Sheets Link

Do not pass the full Google Sheets URL into the library. Extract the spreadsheet ID from the URL and pass that ID to `readSheet`, `readRange`, and `SheetsAccessPolicy`.

Given this link:

```text
https://docs.google.com/spreadsheets/d/1AbCDefGhIjKlMnOpQrStUvWxYz1234567890/edit#gid=0
```

Use only this part:

```text
1AbCDefGhIjKlMnOpQrStUvWxYz1234567890
```

That value goes in two places:

- `SheetsAccessPolicy.allowOnly(spreadsheetIds = setOf(spreadsheetId), ...)`
- `sheetsToSql.readSheet(spreadsheetId = spreadsheetId, sheetName = sheetName)`

## Google Cloud Setup

Create a Google Cloud project:

```bash
gcloud projects create sheets-to-sql-library --name="sheets-to-sql-library"
gcloud config set project sheets-to-sql-library
```

Enable the Google Sheets API:

```bash
gcloud services enable sheets.googleapis.com
```

Enable Google Drive API only if you later add Sheet discovery by name or folder:

```bash
gcloud services enable drive.googleapis.com
```

Create a service account:

```bash
gcloud iam service-accounts create sheets-to-sql-reader \
  --display-name="Sheets to SQL Reader"
```

Create a JSON key:

```bash
mkdir -p secrets
gcloud iam service-accounts keys create ./secrets/sheets-to-sql-reader.json \
  --iam-account=sheets-to-sql-reader@sheets-to-sql-library.iam.gserviceaccount.com
```

Share target spreadsheets with the service account email. Use Viewer access unless you add write-back features.

## Credentials

Prefer Application Default Credentials:

```bash
export GOOGLE_APPLICATION_CREDENTIALS=./secrets/sheets-to-sql-reader.json
```

Or pass a credentials path explicitly:

```kotlin
GoogleSheetsConfig.fromCredentialsFile(Path.of("./secrets/sheets-to-sql-reader.json"))
```

Never commit service account keys. Rotate keys periodically, delete old keys after rotation, and share only the specific Sheets needed by the service account.

## Access Policy

If an outside user can influence `spreadsheetId`, `sheetName`, or `range`, configure an access policy before reading from Google. Otherwise, a backend service account can become a confused deputy that reads any Sheet shared with it.

```kotlin
import io.github.avasiaxx.sheetstosql.google.SheetsAccessPolicy

val sheetsToSql = SheetsToSql.create(
    SheetsToSqlConfig(
        google = GoogleSheetsConfig.fromApplicationDefaultCredentials(
            accessPolicy = SheetsAccessPolicy.allowOnly(
                spreadsheetIds = setOf("approved-spreadsheet-id"),
                sheetNames = setOf("Customers"),
                rangePatterns = listOf(Regex("'Customers'![A-Z]+\\d+:[A-Z]+\\d+"))
            )
        ),
        dialect = PostgresDialect
    )
)
```

For user-facing applications, authorize the user in your application first, then map them to server-side approved Sheet IDs. Do not treat possession of a spreadsheet ID as permission to read it.

`allowedSheetNames` controls full-tab reads through `readSheet`. `allowedRangePatterns` controls raw A1 reads through `readRange`.

The default access policy is deny-all. For a trusted local one-off script, you can explicitly opt out:

```kotlin
SheetsAccessPolicy.allowAllForTrustedLocalUseOnly()
```

## Usage

```kotlin
import io.github.avasiaxx.sheetstosql.SheetsToSql
import io.github.avasiaxx.sheetstosql.config.SheetsToSqlConfig
import io.github.avasiaxx.sheetstosql.google.GoogleSheetsConfig
import io.github.avasiaxx.sheetstosql.google.SheetsAccessPolicy
import io.github.avasiaxx.sheetstosql.sql.PostgresDialect

val sheetsToSql = SheetsToSql.create(
    SheetsToSqlConfig(
        google = GoogleSheetsConfig.fromApplicationDefaultCredentials(
            accessPolicy = SheetsAccessPolicy.allowOnly(
                spreadsheetIds = setOf("your-spreadsheet-id"),
                sheetNames = setOf("Customers")
            )
        ),
        dialect = PostgresDialect
    )
)

val sheet = sheetsToSql.readSheet(
    spreadsheetId = "your-spreadsheet-id",
    sheetName = "Customers"
)

val review = sheetsToSql.reviewSchema(
    sheet = sheet,
    tableName = "customers"
)

review.warnings.forEach(::println)

val createTable = sheetsToSql.generateCreateTable(review.schema)
println(createTable.sql)

val insert = sheetsToSql.generateInsert(
    schema = review.schema,
    rows = sheet.rows
)
println(insert.sql)
println(insert.parameters)
```

`generateInsert` accepts rows keyed by either normalized SQL column names or the original Sheet headers, so the raw `sheet.rows` value can be passed directly.

## Agent Implementation Prompt

Use this prompt in another Kotlin/Gradle project when asking an agent to integrate the library:

```text
Integrate the local Maven dependency `io.github.avasiaxx:sheets-to-sql:0.1.0-SNAPSHOT`.

Add `mavenLocal()` and `mavenCentral()` to Gradle repositories, then add the dependency.

Use Application Default Credentials through `GOOGLE_APPLICATION_CREDENTIALS`.

The Google Sheets input may be provided as a full URL. Extract the spreadsheet ID from the `/d/{spreadsheetId}/` segment before calling the library.

Configure `SheetsAccessPolicy.allowOnly(...)` with the approved spreadsheet ID and exact tab names before reading any Sheet data. Do not use `allowAllForTrustedLocalUseOnly()` in user-facing code.

Read the target tab with `SheetsToSql.readSheet(spreadsheetId, sheetName)`, call `reviewSchema`, inspect warnings/errors, then generate PostgreSQL `CREATE TABLE` and parameterized `INSERT` SQL.

Do not log credentials, spreadsheet contents, SQL parameters, spreadsheet IDs, ranges, or raw Google API responses.
```

## Header Normalization

Examples:

| Sheet Header | SQL Column |
| --- | --- |
| `Customer ID` | `customer_id` |
| `Customer ID` duplicate | `customer_id_2` |
| empty header | `column_1` |
| `123 Amount` | `column_123_amount` |
| `Created At!` | `created_at` |

## Inferred Types

The core inference supports:

- `TEXT`
- `INTEGER`
- `DECIMAL`
- `BOOLEAN`
- `DATE`
- `TIMESTAMP`

Mixed columns fall back to text. Blank values are treated as null by default.

## SQL Dialects

`SqlDialect` is the extension point for database-specific generation.

Implemented:

- PostgreSQL

## Chronicle Atlas Excel Import

SheetsToSQL can also act as a repeatable Chronicle Atlas import foundation while a campaign workbook remains the temporary source of truth. The first supported import targets are `Characters` and `NPCs` from an Excel workbook.

The Chronicle Atlas importer expects the current planner layout:

- Row 1 is a sheet title and is ignored.
- Row 2 contains headers.
- Data starts on row 3.
- Blank formatted rows are ignored, including workbooks formatted down to row 200.
- Cell values are stored as raw text first, without aggressive type normalization.

```kotlin
import io.github.avasiaxx.sheetstosql.chronicleatlas.ChronicleAtlasImporter
import java.nio.file.Path

val report = ChronicleAtlasImporter().importWorkbook(
    workbookPath = Path.of("campaign-planner.xlsx"),
    databasePath = Path.of("chronicle-atlas.db")
)

println("created=${report.createdRows}")
println("updated=${report.updatedRows}")
println("skipped=${report.skippedRows}")
println("warningRows=${report.warningRows}")
```

The importer creates or updates local SQLite tables named `chronicle_atlas_characters` and `chronicle_atlas_npcs`. Each row includes source metadata:

| Column | Purpose |
| --- | --- |
| `_source_sheet` | Workbook sheet used for the import target. |
| `_source_row_number` | 1-based Excel row number from the source workbook. |
| `_source_row_hash` | SHA-256 hash of the source row contents. |
| `_source_entity_key` | Stable repeat-import key, using `Name` by default and row hash when no name is present. |
| `_imported_at` | Import timestamp for created or updated rows. |

Repeated imports use `_source_entity_key` to avoid duplicates. Rows with the same hash and source row number are skipped, changed rows are updated, and new rows are inserted. The import report exposes created, updated, skipped, and warning row counts without printing raw cell values.

Planned:

- MySQL
- SQLite
- SQL Server

## Future Work

- Schema override files
- Column mapping
- Validation rules
- Incremental sync
- Google Drive discovery
- Status sheet write-back
