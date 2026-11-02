package com.example.model

enum class ScanMode(val label: String, val description: String) {
    AUTO_DETECT("Auto-Detect", "Detects bordered tables & itemized lists automatically"),
    STRUCTURED_TABLE("Grid Table", "Optimized for multi-column ruled tables & ledgers"),
    ITEMIZED_LIST("Itemized Receipt", "Splits line items, quantities, units & prices")
}

enum class UnitCategory(val displayName: String, val badgeCode: String) {
    CURRENCY("Currency", "CUR"),
    WEIGHT("Weight / Mass", "WT"),
    VOLUME("Volume", "VOL"),
    LENGTH("Dimension", "DIM"),
    QUANTITY("Quantity / Count", "QTY"),
    PERCENTAGE("Percentage", "PCT"),
    TEMPERATURE("Temperature", "TMP"),
    NUMBER("Numeric", "NUM"),
    TEXT("Text / Label", "TXT")
}

data class DetectedCell(
    val rawText: String,
    val cleanNumber: Double? = null,
    val unitSymbol: String = "",
    val unitCategory: UnitCategory = UnitCategory.TEXT,
    val confidence: Float = 0.96f
) {
    val isNumeric: Boolean
        get() = cleanNumber != null

    fun formattedDisplay(stripUnit: Boolean): String {
        if (!isNumeric) return rawText
        if (stripUnit) {
            val num = cleanNumber ?: return rawText
            return if (num % 1.0 == 0.0) {
                num.toLong().toString()
            } else {
                String.format(java.util.Locale.US, "%.2f", num)
            }
        }
        return rawText
    }
}

data class SheetColumn(
    val name: String,
    val dominantCategory: UnitCategory = UnitCategory.TEXT,
    val detectedUnit: String = ""
) {
    fun headerWithUnit(appendUnit: Boolean): String {
        return if (appendUnit && detectedUnit.isNotBlank() && !name.contains(detectedUnit)) {
            "$name ($detectedUnit)"
        } else {
            name
        }
    }
}

data class ScannedSheet(
    val id: Long = 0L,
    val title: String,
    val subtitle: String,
    val scanMode: ScanMode,
    val createdAt: Long = System.currentTimeMillis(),
    val columns: List<SheetColumn>,
    val rows: List<List<DetectedCell>>,
    val notes: String = "",
    val confidenceScore: Float = 0.97f,
    val paperTemplateId: Int = 0
) {
    val rowCount: Int get() = rows.size
    val columnCount: Int get() = columns.size

    val numericCellCount: Int
        get() = rows.sumOf { row -> row.count { it.isNumeric } }

    val detectedUnitsSummary: List<String>
        get() = rows.flatten()
            .mapNotNull { cell ->
                if (cell.unitSymbol.isNotBlank()) {
                    "${cell.unitSymbol} (${cell.unitCategory.badgeCode})"
                } else null
            }
            .distinct()

    fun columnTotals(): Map<Int, Double?> {
        val result = mutableMapOf<Int, Double?>()
        for (colIdx in columns.indices) {
            val numbers = rows.mapNotNull { row -> row.getOrNull(colIdx)?.cleanNumber }
            if (numbers.isNotEmpty()) {
                result[colIdx] = numbers.sum()
            } else {
                result[colIdx] = null
            }
        }
        return result
    }
}

enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
    XLSX(
        extension = "xlsx",
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        label = "Excel Workbook (.xlsx)"
    ),
    CSV(
        extension = "csv",
        mimeType = "text/csv",
        label = "Comma-Separated (.csv)"
    )
}

data class ExportOptions(
    val format: ExportFormat = ExportFormat.XLSX,
    val stripUnitsFromNumbers: Boolean = true,
    val splitUnitIntoAdjacentColumn: Boolean = false,
    val includeSummaryFormulaRow: Boolean = true
)
