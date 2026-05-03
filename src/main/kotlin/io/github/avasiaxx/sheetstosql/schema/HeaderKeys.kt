package io.github.avasiaxx.sheetstosql.schema

object HeaderKeys {
    fun stable(headers: List<String>, header: String, index: Int): String {
        val safeHeader = header.ifBlank { "column_${index + 1}" }
        val priorMatches = headers.take(index).count { it == header }
        return if (priorMatches == 0) safeHeader else "$safeHeader#${priorMatches + 1}"
    }
}
