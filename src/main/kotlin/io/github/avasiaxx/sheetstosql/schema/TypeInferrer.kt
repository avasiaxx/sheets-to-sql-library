package io.github.avasiaxx.sheetstosql.schema

import io.github.avasiaxx.sheetstosql.model.SqlType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object TypeInferrer {
    private val trueValues = setOf("true", "t", "yes", "y")
    private val falseValues = setOf("false", "f", "no", "n")
    private val dateFormats = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("M/d/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("M/d/yy"),
        DateTimeFormatter.ofPattern("MM/dd/yy")
    )

    fun infer(values: List<String?>, blankValuePolicy: BlankValuePolicy = BlankValuePolicy.default()): SqlType {
        val present = values.mapNotNull { blankValuePolicy.normalize(it)?.trim() }
        if (present.isEmpty()) return SqlType.TEXT

        return when {
            present.all(::isBoolean) -> SqlType.BOOLEAN
            present.all(::isInteger) -> SqlType.INTEGER
            present.all(::isDecimal) -> SqlType.DECIMAL
            present.all(::isDate) -> SqlType.DATE
            present.all(::isTimestamp) -> SqlType.TIMESTAMP
            else -> SqlType.TEXT
        }
    }

    private fun isBoolean(value: String): Boolean = value.lowercase() in trueValues || value.lowercase() in falseValues

    private fun isInteger(value: String): Boolean = value.toLongOrNull() != null

    private fun isDecimal(value: String): Boolean = try {
        BigDecimal(value)
        true
    } catch (_: NumberFormatException) {
        false
    }

    private fun isDate(value: String): Boolean = dateFormats.any { formatter ->
        try {
            LocalDate.parse(value, formatter)
            true
        } catch (_: DateTimeParseException) {
            false
        }
    }

    private fun isTimestamp(value: String): Boolean = try {
        OffsetDateTime.parse(value)
        true
    } catch (_: DateTimeParseException) {
        false
    }
}
