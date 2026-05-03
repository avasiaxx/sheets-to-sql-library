package io.github.avasiaxx.sheetstosql.errors

open class SheetsToSqlException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class SheetReadException(message: String, cause: Throwable? = null) : SheetsToSqlException(message, cause)

class SchemaException(message: String) : SheetsToSqlException(message)

class AccessDeniedException(message: String) : SheetsToSqlException(message)
