package io.github.avasiaxx.sheetstosql.schema

data class NormalizedHeaders(
    val headers: List<String>,
    val warnings: List<String>
)

object HeaderNormalizer {
    fun normalize(
        headers: List<String>,
        reservedIdentifiers: Set<String> = emptySet()
    ): NormalizedHeaders {
        val baseCounts = mutableMapOf<String, Int>()
        val used = reservedIdentifiers.toMutableSet()
        val warnings = mutableListOf<String>()

        val normalized = headers.mapIndexed { index, header ->
            val base = normalizeOne(header, index)
            if (header.isBlank()) {
                warnings += "Empty header at column ${index + 1} renamed to '$base'"
            } else if (base != header) {
                warnings += "Header '$header' normalized to '$base'"
            }

            val count = baseCounts.getOrDefault(base, 0) + 1
            baseCounts[base] = count
            var candidate = if (count == 1) base else "${base}_$count"
            var suffix = count + 1
            while (candidate in used) {
                candidate = "${base}_${suffix++}"
            }
            used += candidate
            if (candidate != base) {
                warnings += if (count > 1) {
                    "Duplicate header '$header' renamed to '$candidate'"
                } else {
                    "Reserved header '$header' renamed to '$candidate'"
                }
            }
            candidate
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
