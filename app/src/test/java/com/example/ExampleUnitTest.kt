package com.example

import com.example.domain.PaperBitmapGenerator
import com.example.domain.UnitDetector
import com.example.export.SpreadsheetExporter
import com.example.model.ExportFormat
import com.example.model.ExportOptions
import com.example.model.UnitCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class ExampleUnitTest {

    @Test
    fun unitDetector_parsesCurrenciesWeightsVolumesAndQuantities() {
        val currencyCell = UnitDetector.parseCell("$1,245.80")
        assertTrue(currencyCell.isNumeric)
        assertEquals(1245.80, currencyCell.cleanNumber ?: 0.0, 0.001)
        assertEquals("$", currencyCell.unitSymbol)
        assertEquals(UnitCategory.CURRENCY, currencyCell.unitCategory)

        val weightCell = UnitDetector.parseCell("42.5 kg")
        assertTrue(weightCell.isNumeric)
        assertEquals(42.5, weightCell.cleanNumber ?: 0.0, 0.001)
        assertEquals("kg", weightCell.unitSymbol)
        assertEquals(UnitCategory.WEIGHT, weightCell.unitCategory)

        val volumeCell = UnitDetector.parseCell("1250 mL")
        assertTrue(volumeCell.isNumeric)
        assertEquals(1250.0, volumeCell.cleanNumber ?: 0.0, 0.001)
        assertEquals("mL", volumeCell.unitSymbol)
        assertEquals(UnitCategory.VOLUME, volumeCell.unitCategory)

        val qtyCell = UnitDetector.parseCell("24 pcs")
        assertTrue(qtyCell.isNumeric)
        assertEquals(24.0, qtyCell.cleanNumber ?: 0.0, 0.001)
        assertEquals("pcs", qtyCell.unitSymbol)
        assertEquals(UnitCategory.QUANTITY, qtyCell.unitCategory)
    }

    @Test
    fun unitDetector_convertsUnitsAccurately() {
        val kgCell = UnitDetector.parseCell("10 kg")
        val toLbOption = UnitDetector.availableConversionsFor("kg").first { it.targetUnit == "lb" }
        val converted = UnitDetector.convertCell(kgCell, toLbOption)
        assertEquals("lb", converted.unitSymbol)
        assertEquals(22.05, converted.cleanNumber ?: 0.0, 0.05)
    }

    @Test
    fun spreadsheetExporter_generatesValidXlsxAndCsvWithFormulas() {
        val spec = PaperBitmapGenerator.samplePapers.first()
        val sheet = PaperBitmapGenerator.buildSheetFromSpec(spec)

        // Test CSV generation
        val csv = SpreadsheetExporter.buildCsvString(
            sheet = sheet,
            options = ExportOptions(
                format = ExportFormat.CSV,
                stripUnitsFromNumbers = true,
                includeSummaryFormulaRow = true
            )
        )
        assertTrue(csv.contains("Item Description"))
        assertTrue(csv.contains("Line Total ($)"))
        assertTrue(csv.contains("TOTAL"))

        // Test OpenXML .xlsx ZIP generation
        val xlsxBytes = SpreadsheetExporter.buildXlsxBytes(
            sheet = sheet,
            options = ExportOptions(
                format = ExportFormat.XLSX,
                stripUnitsFromNumbers = true,
                includeSummaryFormulaRow = true
            )
        )
        assertTrue(xlsxBytes.isNotEmpty())

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(xlsxBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readBytes().toString(Charsets.UTF_8)
                entry = zis.nextEntry
            }
        }
        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("xl/workbook.xml"))
        val worksheetXml = entries["xl/worksheets/sheet1.xml"]
        assertNotNull(worksheetXml)
        assertTrue(worksheetXml!!.contains("<f>SUM("))
    }
}
