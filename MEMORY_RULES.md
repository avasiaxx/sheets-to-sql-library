# Memory Rules

Use this file to keep project memories simple, factual, and actionable.

## Read Order

When working in this repository, read Markdown files in this order:

1. `AGENTS.md` - security rules and agent behavior.
2. `MEMORY_RULES.md` - what project memories should remember and how to use them.
3. `README.md` - installation, Maven Local import, Google Sheets setup, access policy, and usage examples.

## What To Remember

Keep memories limited to stable project facts:

- This is a Kotlin/JVM library named `sheets-to-sql`.
- Maven coordinates are `io.github.avasiaxx:sheets-to-sql:0.1.0-SNAPSHOT`.
- Kotlin package namespace is `io.github.avasiaxx.sheetstosql`.
- The library reads Google Sheets, reviews/infer schemas, and generates SQL.
- PostgreSQL is the first implemented SQL dialect.
- Google access policy defaults to deny-all.
- User-facing integrations must use `SheetsAccessPolicy.allowOnly(...)`.
- Spreadsheet URLs must be converted to spreadsheet IDs before calling the library.
- Credentials must come from `GOOGLE_APPLICATION_CREDENTIALS` or an explicit credentials path.
- Service account JSON keys, `.env` files, Sheet exports, and generated data dumps must never be committed.

## What Not To Remember

Do not store sensitive or unstable data in memory:

- Service account emails unless the user explicitly asks and confirms they are safe to store.
- Service account JSON contents.
- Access tokens, refresh tokens, API keys, passwords, or credential paths containing usernames/private folders.
- Spreadsheet IDs from private or client data unless the user explicitly asks and confirms they are safe to store.
- Sheet cell values, generated SQL parameters, raw Google API responses, or database credentials.

## How To Use This Project

For another project to consume this library:

1. Run `./gradlew publishToMavenLocal` from this repository.
2. Add `mavenLocal()` and `mavenCentral()` to the consuming project repositories.
3. Add `implementation("io.github.avasiaxx:sheets-to-sql:0.1.0-SNAPSHOT")`.
4. Extract the spreadsheet ID from the Google Sheets URL `/d/{spreadsheetId}/` segment.
5. Configure `SheetsAccessPolicy.allowOnly(...)` with the approved spreadsheet ID and tab names.
6. Use `SheetsToSql.readSheet(...)`, `reviewSchema(...)`, `generateCreateTable(...)`, and `generateInsert(...)`.

## Memory Format

When saving a memory about this project, prefer this format:

```text
Project: sheets-to-sql
Fact: <stable fact only>
Security note: <credential/access/data handling constraint if relevant>
Source: <README.md, AGENTS.md, or MEMORY_RULES.md>
```

Keep memories short. Prefer links to `.md` files over copying long instructions into memory.