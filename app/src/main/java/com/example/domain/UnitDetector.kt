package com.example.domain

import com.example.model.DetectedCell
import com.example.model.SheetColumn
import com.example.model.UnitCategory
import java.util.Locale

object UnitDetector {

    private val prefixCurrencies = mapOf(
        "$" to "$",
        "US$" to "USD",
        "USD" to "USD",
        "€" to "€",
        "EUR" to "EUR",
        "£" to "£",
        "GBP" to "GBP",
        "¥" to "¥",
        "JPY" to "JPY",
        "₹" to "₹",
        "INR" to "INR",
        "AED" to "AED",
        "CAD" to "CAD",
        "AUD" to "AUD"
    )

    private val unitLookup: Map<String, Pair<String, UnitCategory>> = mapOf(
        // Currency suffixes
        "usd" to ("USD" to UnitCategory.CURRENCY),
        "eur" to ("EUR" to UnitCategory.CURRENCY),
        "gbp" to ("GBP" to UnitCategory.CURRENCY),
        "inr" to ("INR" to UnitCategory.CURRENCY),
        "jpy" to ("JPY" to UnitCategory.CURRENCY),
        "aed" to ("AED" to UnitCategory.CURRENCY),
        "cad" to ("CAD" to UnitCategory.CURRENCY),
        "$" to ("$" to UnitCategory.CURRENCY),
        "€" to ("€" to UnitCategory.CURRENCY),
        "£" to ("£" to UnitCategory.CURRENCY),
        "₹" to ("₹" to UnitCategory.CURRENCY),
        "¥" to ("¥" to UnitCategory.CURRENCY),

        // Weight / Mass
        "kg" to ("kg" to UnitCategory.WEIGHT),
        "kgs" to ("kg" to UnitCategory.WEIGHT),
        "g" to ("g" to UnitCategory.WEIGHT),
        "gm" to ("g" to UnitCategory.WEIGHT),
        "grams" to ("g" to UnitCategory.WEIGHT),
        "mg" to ("mg" to UnitCategory.WEIGHT),
        "lb" to ("lb" to UnitCategory.WEIGHT),
        "lbs" to ("lb" to UnitCategory.WEIGHT),
        "oz" to ("oz" to UnitCategory.WEIGHT),
        "ton" to ("t" to UnitCategory.WEIGHT),
        "t" to ("t" to UnitCategory.WEIGHT),

        // Volume
        "l" to ("L" to UnitCategory.VOLUME),
        "ltr" to ("L" to UnitCategory.VOLUME),
        "ml" to ("mL" to UnitCategory.VOLUME),
        "gal" to ("gal" to UnitCategory.VOLUME),
        "floz" to ("fl oz" to UnitCategory.VOLUME),

        // Length / Dimension
        "m" to ("m" to UnitCategory.LENGTH),
        "cm" to ("cm" to UnitCategory.LENGTH),
        "mm" to ("mm" to UnitCategory.LENGTH),
        "km" to ("km" to UnitCategory.LENGTH),
        "ft" to ("ft" to UnitCategory.LENGTH),
        "in" to ("in" to UnitCategory.LENGTH),
        "yd" to ("yd" to UnitCategory.LENGTH),

        // Quantity / Count / Time
        "pcs" to ("pcs" to UnitCategory.QUANTITY),
        "pc" to ("pcs" to UnitCategory.QUANTITY),
        "units" to ("units" to UnitCategory.QUANTITY),
        "unit" to ("units" to UnitCategory.QUANTITY),
        "box" to ("box" to UnitCategory.QUANTITY),
        "boxes" to ("box" to UnitCategory.QUANTITY),
        "pk" to ("pk" to UnitCategory.QUANTITY),
        "pack" to ("pk" to UnitCategory.QUANTITY),
        "bags" to ("bag" to UnitCategory.QUANTITY),
        "bag" to ("bag" to UnitCategory.QUANTITY),
        "rolls" to ("roll" to UnitCategory.QUANTITY),
        "roll" to ("roll" to UnitCategory.QUANTITY),
        "hrs" to ("hrs" to UnitCategory.QUANTITY),
        "hr" to ("hrs" to UnitCategory.QUANTITY),
        "days" to ("days" to UnitCategory.QUANTITY),
        "qty" to ("qty" to UnitCategory.QUANTITY),
        "x" to ("x" to UnitCategory.QUANTITY),

        // Percentage
        "%" to ("%" to UnitCategory.PERCENTAGE),
        "pct" to ("%" to UnitCategory.PERCENTAGE),

        // Temperature
        "°c" to ("°C" to UnitCategory.TEMPERATURE),
        "c" to ("°C" to UnitCategory.TEMPERATURE),
        "°f" to ("°F" to UnitCategory.TEMPERATURE)
    )

    // Matches prefix currency/sign + number + optional suffix unit
    private val prefixCurrencyRegex = Regex(
        """^\s*\(?([+-]?)\s*(US\$|USD|EUR|GBP|INR|JPY|AED|CAD|AUD|[\$€£¥₹])\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]+)?|[0-9]+(?:\.[0-9]+)?)\)?\s*$""",
        RegexOption.IGNORE_CASE
    )

    // Matches number + optional suffix unit (e.g. "42.5 kg", "1,250.00 USD", "18.5%", "12 pcs")
    private val numberWithSuffixRegex = Regex(
        """^\s*\(?([+-]?)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]+)?|[0-9]+(?:\.[0-9]+)?)\s*([a-zA-Z°%€£$¥₹]{1,6})?\)?\s*$"""
    )

    fun parseCell(rawInput: String, confidence: Float = 0.96f): DetectedCell {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return DetectedCell(rawText = "", confidence = confidence)
        }

        val isAccountingNegative = trimmed.startsWith("(") && trimmed.endsWith(")")

        // 1. Check Prefix Currency (e.g., "$1,245.80", "€ 49.00", "USD 320.50")
        val prefixMatch = prefixCurrencyRegex.matchEntire(trimmed)
        if (prefixMatch != null) {
            val signStr = prefixMatch.groupValues[1]
            val rawCurrency = prefixMatch.groupValues[2].uppercase(Locale.US)
            val numStr = prefixMatch.groupValues[3].replace(",", "")
            val parsedVal = numStr.toDoubleOrNull()
            if (parsedVal != null) {
                val signedVal = if (signStr == "-" || isAccountingNegative) -parsedVal else parsedVal
                val normalizedUnit = prefixCurrencies[rawCurrency] ?: prefixCurrencies[ prefixMatch.groupValues[2] ] ?: rawCurrency
                return DetectedCell(
                    rawText = trimmed,
                    cleanNumber = signedVal,
                    unitSymbol = normalizedUnit,
                    unitCategory = UnitCategory.CURRENCY,
                    confidence = confidence
                )
            }
        }

        // 2. Check Number with optional Suffix Unit (e.g., "42.5 kg", "98.4%", "24 pcs", "350.00")
        val suffixMatch = numberWithSuffixRegex.matchEntire(trimmed)
        if (suffixMatch != null) {
            val signStr = suffixMatch.groupValues[1]
            val numStr = suffixMatch.groupValues[2].replace(",", "")
            val rawUnit = suffixMatch.groupValues[3].trim()
            val parsedVal = numStr.toDoubleOrNull()
            if (parsedVal != null) {
                val signedVal = if (signStr == "-" || isAccountingNegative) -parsedVal else parsedVal
                if (rawUnit.isEmpty()) {
                    return DetectedCell(
                        rawText = trimmed,
                        cleanNumber = signedVal,
                        unitSymbol = "",
                        unitCategory = UnitCategory.NUMBER,
                        confidence = confidence
                    )
                }
                val lookup = unitLookup[rawUnit.lowercase(Locale.US)]
                if (lookup != null) {
                    return DetectedCell(
                        rawText = trimmed,
                        cleanNumber = signedVal,
                        unitSymbol = lookup.first,
                        unitCategory = lookup.second,
                        confidence = confidence
                    )
                }
            }
        }

        // Default: Text / Label cell
        return DetectedCell(
            rawText = trimmed,
            cleanNumber = null,
            unitSymbol = "",
            unitCategory = UnitCategory.TEXT,
            confidence = confidence
        )
    }

    fun inferColumnMetadata(headerName: String, columnCells: List<DetectedCell>): SheetColumn {
        val numericCells = columnCells.filter { it.isNumeric }
        if (numericCells.isEmpty()) {
            return SheetColumn(
                name = headerName,
                dominantCategory = UnitCategory.TEXT,
                detectedUnit = ""
            )
        }

        val dominantCategory = numericCells
            .groupingBy { it.unitCategory }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: UnitCategory.NUMBER

        val dominantUnit = numericCells
            .map { it.unitSymbol }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: ""

        return SheetColumn(
            name = headerName,
            dominantCategory = dominantCategory,
            detectedUnit = dominantUnit
        )
    }

    data class ConversionOption(
        val targetUnit: String,
        val factor: Double,
        val label: String
    )

    fun availableConversionsFor(unitSymbol: String): List<ConversionOption> {
        return when (unitSymbol) {
            "kg" -> listOf(
                ConversionOption("lb", 2.20462, "kg → lb (×2.205)"),
                ConversionOption("g", 1000.0, "kg → g (×1000)")
            )
            "lb" -> listOf(
                ConversionOption("kg", 0.453592, "lb → kg (×0.454)"),
                ConversionOption("oz", 16.0, "lb → oz (×16)")
            )
            "g" -> listOf(
                ConversionOption("kg", 0.001, "g → kg (÷1000)"),
                ConversionOption("oz", 0.035274, "g → oz (×0.035)")
            )
            "oz" -> listOf(
                ConversionOption("g", 28.3495, "oz → g (×28.35)"),
                ConversionOption("lb", 0.0625, "oz → lb (÷16)")
            )
            "L" -> listOf(
                ConversionOption("mL", 1000.0, "L → mL (×1000)"),
                ConversionOption("gal", 0.264172, "L → gal (×0.264)")
            )
            "mL" -> listOf(
                ConversionOption("L", 0.001, "mL → L (÷1000)"),
                ConversionOption("fl oz", 0.033814, "mL → fl oz")
            )
            "m" -> listOf(
                ConversionOption("cm", 100.0, "m → cm (×100)"),
                ConversionOption("ft", 3.28084, "m → ft (×3.281)")
            )
            "cm" -> listOf(
                ConversionOption("m", 0.01, "cm → m (÷100)"),
                ConversionOption("in", 0.393701, "cm → in (×0.394)")
            )
            "$" -> listOf(
                ConversionOption("€", 0.92, "$ → € (0.92)"),
                ConversionOption("£", 0.79, "$ → £ (0.79)")
            )
            "€" -> listOf(
                ConversionOption("$", 1.09, "€ → $ (1.09)"),
                ConversionOption("£", 0.86, "€ → £ (0.86)")
            )
            else -> emptyList()
        }
    }

    fun convertCell(cell: DetectedCell, option: ConversionOption): DetectedCell {
        val currentVal = cell.cleanNumber ?: return cell
        val convertedVal = currentVal * option.factor
        val formattedVal = String.format(Locale.US, "%.2f", convertedVal)
        val newRaw = if (cell.unitCategory == UnitCategory.CURRENCY &&
            (option.targetUnit == "$" || option.targetUnit == "€" || option.targetUnit == "£")
        ) {
            "${option.targetUnit}$formattedVal"
        } else {
            "$formattedVal ${option.targetUnit}"
        }
        return parseCell(newRaw, cell.confidence)
    }
}
