package io.github.avasiaxx.sheetstosql.config

import io.github.avasiaxx.sheetstosql.google.GoogleSheetsConfig
import io.github.avasiaxx.sheetstosql.schema.BlankValuePolicy
import io.github.avasiaxx.sheetstosql.sql.PostgresDialect
import io.github.avasiaxx.sheetstosql.sql.SqlDialect

data class SheetsToSqlConfig(
    val google: GoogleSheetsConfig = GoogleSheetsConfig.fromApplicationDefaultCredentials(),
    val dialect: SqlDialect = PostgresDialect,
    val blankValuePolicy: BlankValuePolicy = BlankValuePolicy.default()
)
