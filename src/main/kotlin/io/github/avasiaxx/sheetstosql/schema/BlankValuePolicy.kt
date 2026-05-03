package io.github.avasiaxx.sheetstosql.schema

data class BlankValuePolicy(
    val blankAsNull: Boolean = true,
    val blankTokens: Set<String> = setOf("", "NULL", "N/A", "NA")
) {
    fun normalize(value: String?): String? {
        if (value == null) return null
        val trimmed = value.trim()
        val isBlankToken = blankTokens.any { it.equals(trimmed, ignoreCase = true) }
        return if (blankAsNull && isBlankToken) null else value
    }

    companion object {
        fun default(): BlankValuePolicy = BlankValuePolicy()
    }
}
