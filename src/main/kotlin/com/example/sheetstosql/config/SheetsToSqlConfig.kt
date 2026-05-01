package com.example.sheetstosql.config

import com.example.sheetstosql.google.GoogleSheetsConfig
import com.example.sheetstosql.schema.BlankValuePolicy
import com.example.sheetstosql.sql.PostgresDialect
import com.example.sheetstosql.sql.SqlDialect

data class SheetsToSqlConfig(
    val google: GoogleSheetsConfig = GoogleSheetsConfig.fromApplicationDefaultCredentials(),
    val dialect: SqlDialect = PostgresDialect,
    val blankValuePolicy: BlankValuePolicy = BlankValuePolicy.default()
)
