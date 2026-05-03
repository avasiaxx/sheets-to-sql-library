package io.github.avasiaxx.sheetstosql.schema

data class NormalizedHeaders(
    val headers: List<String>,
    val warnings: List<String>
)

object HeaderNormalizer {
    fun normalize(headers: List<String>): NormalizedHeaders {
        val seen = mutableMapOf<String, Int>()
        val warnings = mutableListOf<String>()

        val normalized = headers.mapIndexed { index, header ->
            val base = normalizeOne(header, index)
            if (header.isBlank()) {
                warnings += "Empty header at column ${index + 1} renamed to '$base'"
            } else if (base != header) {
                warnings += "Header '$header' normalized to '$base'"
            }

            val count = seen.getOrDefault(base, 0) + 1
            seen[base] = count
            if (count == 1) {
                base
            } else {
                val deduped = "${base}_$count"
                warnings += "Duplicate header '$header' renamed to '$deduped'"
                deduped
            }
        }

        return NormalizedHeaders(normalized, warnings)
    }

    private fun normalizeOne(header: String, index: Int): String {
        val cleaned = header
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .replace(Regex("_+"), "_")

        val fallback = if (cleaned.isBlank()) "column_${index + 1}" else cleaned
        return if (fallback.first().isDigit()) "column_$fallback" else fallback
    }
}
