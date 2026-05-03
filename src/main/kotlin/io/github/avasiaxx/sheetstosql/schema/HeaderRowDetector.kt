package io.github.avasiaxx.sheetstosql.schema

object HeaderRowDetector {
    fun detect(rows: List<List<String?>>, maxRowsToScan: Int = 10): Int {
        return rows
            .take(maxRowsToScan)
            .withIndex()
            .maxByOrNull { (_, row) ->
                row.count { !it.isNullOrBlank() }
            }
            ?.index ?: 0
    }
}
