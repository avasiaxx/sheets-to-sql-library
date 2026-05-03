# AGENTS Security Standard

This repository builds a library that can read Google Sheets using service account credentials and generate SQL. Treat all spreadsheet content, credentials, generated SQL parameters, logs, and schema reports as potentially sensitive.

## Prime Directive

Security is the highest priority. If a requested change improves convenience but weakens credential handling, access control, data minimization, SQL safety, or auditability, do not implement it without an explicit documented security review.


## Project Memory Rules

Project memories must follow `MEMORY_RULES.md`. Read it after this file and before `README.md` when starting work in this repository.

## Required Practices

- Never commit service account JSON keys, access tokens, `.env` files, exported Sheet data, generated SQL data dumps, or local credential paths.
- Keep Google OAuth scopes read-only unless a feature explicitly requires write access and has a separate review.
- Do not add Google Drive API usage unless discovery by name, folder, owner, or metadata is required. Drive access broadens the data-discovery surface.
- Do not log spreadsheet cell values, SQL parameter values, credentials, authorization headers, full service account JSON, or raw API responses.
- Do not include spreadsheet IDs, sheet names, or ranges in exception messages unless they are explicitly marked safe by the caller.
- Keep SQL generation parameterized by default. Do not interpolate cell values into SQL strings.
- Quote SQL identifiers through the active dialect only. Never concatenate untrusted table or column names without dialect quoting and validation.
- Treat `spreadsheetId`, `sheetName`, and `range` as untrusted whenever they can come from an outside user.
- Enforce an access policy before calling Google APIs in any code path that reads spreadsheet metadata, tabs, ranges, or cells.
- Default to least privilege: specific Sheet sharing with Viewer permission, no broad Drive access, and no domain-wide delegation.
- Add tests for security-relevant behavior whenever changing auth, access policy, SQL generation, logging, exceptions, or file handling.

## Outside User Data Access Risks

The most important liability is confused-deputy access: a backend service using this library may have a service account that can read many Sheets. If an outside user can choose a `spreadsheetId`, `sheetName`, or `range`, they may cause the backend to read data they should not access.

Mitigations:

- Require consuming apps to map users to approved spreadsheet IDs server-side.
- Use `SheetsAccessPolicy` allowlists for spreadsheet IDs, sheet names, and ranges.
- Do not expose raw spreadsheet IDs as an authorization mechanism.
- Do not enable Drive discovery for user-facing searches without separate authorization checks.
- Keep service accounts narrowly shared only to Sheets the application should read.

## Review Checklist

Before finalizing any change, verify:

- Credentials remain out of source control and logs.
- Google scopes are still least-privilege.
- All Google read paths enforce access policy.
- Generated SQL remains parameterized for values.
- Exceptions do not disclose data or secrets.
- Tests cover the changed behavior.
- README/security docs are updated if setup or risk changes.

## Publication Rules

Before publishing this library:

- Confirm the Maven group and Kotlin package namespace match the intended publishing identity.
- Confirm `gradle-wrapper.properties` includes `distributionSha256Sum` and the wrapper JAR checksum matches Gradle's published checksum.
- Run `./gradlew test`.
- Review dependencies for known vulnerabilities.
- Confirm `.gitignore` excludes credential and data-export artifacts.
- Confirm examples use placeholders and environment variables only.
- Keep CI and dependency-update automation enabled before accepting outside contributions.
