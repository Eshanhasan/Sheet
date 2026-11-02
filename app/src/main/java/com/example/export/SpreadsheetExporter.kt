package com.example.export

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.DetectedCell
import com.example.model.ExportFormat
import com.example.model.ExportOptions
import com.example.model.ScannedSheet
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SpreadsheetExporter {

    fun sanitizeFileName(title: String, format: ExportFormat): String {
        val base = title
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "sheetsnap_table" }
        return "${base}.${format.extension}"
    }

    /**
     * Generates the .xlsx or .csv file into the app's FileProvider cache directory (`cacheDir/exports/`).
     */
    fun exportToCacheFile(
        context: Context,
        sheet: ScannedSheet,
        options: ExportOptions
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = sanitizeFileName(sheet.title, options.format)
        val outFile = File(exportDir, fileName)
        FileOutputStream(outFile).use { fos ->
            writeSheetToStream(fos, sheet, options)
        }
        return outFile
    }

    /**
     * Writes the .xlsx or .csv directly to a user-selected Cloud Storage / SAF URI
     * (Google Drive, OneDrive, Dropbox, or Device Documents).
     */
    fun writeSheetToUri(
        context: Context,
        uri: Uri,
        sheet: ScannedSheet,
        options: ExportOptions
    ): Result<String> {
        return runCatching {
            val stream = context.contentResolver.openOutputStream(uri)
                ?: error("Unable to open output stream for selected destination.")
            stream.use { out ->
                writeSheetToStream(out, sheet, options)
            }
            sanitizeFileName(sheet.title, options.format)
        }
    }

    fun writeSheetToStream(
        outputStream: OutputStream,
        sheet: ScannedSheet,
        options: ExportOptions
    ) {
        when (options.format) {
            ExportFormat.CSV -> {
                val csvBytes = buildCsvBytes(sheet, options)
                outputStream.write(csvBytes)
                outputStream.flush()
            }
            ExportFormat.XLSX -> {
                val xlsxBytes = buildXlsxBytes(sheet, options)
                outputStream.write(xlsxBytes)
                outputStream.flush()
            }
        }
    }

    /**
     * Builds RFC-4180 compliant CSV bytes with UTF-8 BOM so Excel & Google Sheets open currency/unit symbols cleanly.
     */
    fun buildCsvString(sheet: ScannedSheet, options: ExportOptions): String {
        val sb = StringBuilder()
        val expandedHeaders = buildExpandedHeaders(sheet, options)
        sb.append(expandedHeaders.joinToString(",") { escapeCsvField(it) }).append("\r\n")

        for (row in sheet.rows) {
            val rowValues = mutableListOf<String>()
            for (colIdx in sheet.columns.indices) {
                val cell = row.getOrNull(colIdx) ?: DetectedCell("")
                if (options.splitUnitIntoAdjacentColumn && sheet.columns[colIdx].detectedUnit.isNotBlank()) {
                    val numStr = cell.cleanNumber?.let { formatCleanNumber(it) } ?: cell.rawText
                    rowValues.add(escapeCsvField(numStr))
                    rowValues.add(escapeCsvField(cell.unitSymbol))
                } else if (options.stripUnitsFromNumbers && cell.isNumeric) {
                    rowValues.add(escapeCsvField(formatCleanNumber(cell.cleanNumber!!)))
                } else {
                    rowValues.add(escapeCsvField(cell.rawText))
                }
            }
            sb.append(rowValues.joinToString(",")).append("\r\n")
        }

        if (options.includeSummaryFormulaRow && sheet.rows.isNotEmpty()) {
            val totals = sheet.columnTotals()
            val footerValues = mutableListOf<String>()
            for (colIdx in sheet.columns.indices) {
                val colTotal = totals[colIdx]
                val unit = sheet.columns[colIdx].detectedUnit
                if (colIdx == 0 && colTotal == null) {
                    footerValues.add(escapeCsvField("TOTAL"))
                } else if (options.splitUnitIntoAdjacentColumn && unit.isNotBlank()) {
                    footerValues.add(escapeCsvField(colTotal?.let { formatCleanNumber(it) } ?: ""))
                    footerValues.add(escapeCsvField(unit))
                } else if (colTotal != null) {
                    val formatted = if (options.stripUnitsFromNumbers || unit.isBlank()) {
                        formatCleanNumber(colTotal)
                    } else if (unit == "$" || unit == "€" || unit == "£" || unit == "¥" || unit == "₹") {
                        "$unit${formatCleanNumber(colTotal)}"
                    } else {
                        "${formatCleanNumber(colTotal)} $unit"
                    }
                    footerValues.add(escapeCsvField(formatted))
                } else {
                    footerValues.add("")
                }
            }
            sb.append(footerValues.joinToString(",")).append("\r\n")
        }

        return sb.toString()
    }

    private fun buildCsvBytes(sheet: ScannedSheet, options: ExportOptions): ByteArray {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val content = buildCsvString(sheet, options).toByteArray(StandardCharsets.UTF_8)
        return bom + content
    }

    /**
     * Generates a standards-compliant Office Open XML (.xlsx) SpreadsheetML ZIP archive
     * with styled emerald headers, numeric cells, and real SUM() formulas.
     */
    fun buildXlsxBytes(sheet: ScannedSheet, options: ExportOptions): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // 1. [Content_Types].xml
            putZipEntry(
                zos,
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""
            )

            // 2. _rels/.rels
            putZipEntry(
                zos,
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            )

            // 3. xl/workbook.xml
            val safeSheetName = xmlEscape(sheet.title.take(28).replace(Regex("[\\\\/?*\\[\\]:]"), " ").ifBlank { "ExtractedData" })
            putZipEntry(
                zos,
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="$safeSheetName" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""
            )

            // 4. xl/_rels/workbook.xml.rels
            putZipEntry(
                zos,
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
            )

            // 5. xl/styles.xml (0 = Normal, 1 = Header Emerald Fill + Bold White, 2 = Numeric Cell, 3 = Total Row Bold)
            putZipEntry(
                zos,
                "xl/styles.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="3">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFF0FDF4"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FF065F46"/><name val="Calibri"/></font>
  </fonts>
  <fills count="4">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF0F2920"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFECFDF5"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFCBD5E1"/></left>
      <right style="thin"><color rgb="FFCBD5E1"/></right>
      <top style="thin"><color rgb="FFCBD5E1"/></top>
      <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="4">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
    <xf numFmtId="2" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/>
    <xf numFmtId="2" fontId="2" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1"/>
  </cellXfs>
</styleSheet>"""
            )

            // 6. xl/worksheets/sheet1.xml
            val sheetXml = buildWorksheetXml(sheet, options)
            putZipEntry(zos, "xl/worksheets/sheet1.xml", sheetXml)
        }
        return baos.toByteArray()
    }

    private fun buildWorksheetXml(sheet: ScannedSheet, options: ExportOptions): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

        val headers = buildExpandedHeaders(sheet, options)
        sb.append("<cols>")
        for (i in 1..headers.size) {
            val width = if (i <= 2) 24 else 16
            sb.append("""<col min="$i" max="$i" width="$width" customWidth="1"/>""")
        }
        sb.append("</cols>")

        sb.append("<sheetData>")

        // Row 1: Headers
        sb.append("""<row r="1" ht="24" customHeight="1">""")
        headers.forEachIndexed { cIdx, hText ->
            val cellRef = "${excelColLetter(cIdx)}1"
            sb.append("""<c r="$cellRef" s="1" t="inlineStr"><is><t>${xmlEscape(hText)}</t></is></c>""")
        }
        sb.append("</row>")

        // Data Rows (Rows 2 .. N+1)
        val numericExcelColIndices = mutableSetOf<Int>()
        sheet.rows.forEachIndexed { rIdx, rowCells ->
            val excelRowNum = rIdx + 2
            sb.append("""<row r="$excelRowNum">""")
            var outColIdx = 0
            for (srcColIdx in sheet.columns.indices) {
                val cell = rowCells.getOrNull(srcColIdx) ?: DetectedCell("")
                val colMeta = sheet.columns[srcColIdx]
                val cellRef = "${excelColLetter(outColIdx)}$excelRowNum"

                if (options.splitUnitIntoAdjacentColumn && colMeta.detectedUnit.isNotBlank()) {
                    if (cell.cleanNumber != null) {
                        numericExcelColIndices.add(outColIdx)
                        sb.append("""<c r="$cellRef" s="2" t="n"><v>${cell.cleanNumber}</v></c>""")
                    } else {
                        sb.append("""<c r="$cellRef" s="0" t="inlineStr"><is><t>${xmlEscape(cell.rawText)}</t></is></c>""")
                    }
                    outColIdx++
                    val unitRef = "${excelColLetter(outColIdx)}$excelRowNum"
                    sb.append("""<c r="$unitRef" s="0" t="inlineStr"><is><t>${xmlEscape(cell.unitSymbol)}</t></is></c>""")
                    outColIdx++
                } else if (options.stripUnitsFromNumbers && cell.cleanNumber != null) {
                    numericExcelColIndices.add(outColIdx)
                    sb.append("""<c r="$cellRef" s="2" t="n"><v>${cell.cleanNumber}</v></c>""")
                    outColIdx++
                } else if (cell.cleanNumber != null && cell.unitSymbol.isBlank()) {
                    numericExcelColIndices.add(outColIdx)
                    sb.append("""<c r="$cellRef" s="2" t="n"><v>${cell.cleanNumber}</v></c>""")
                    outColIdx++
                } else {
                    sb.append("""<c r="$cellRef" s="0" t="inlineStr"><is><t>${xmlEscape(cell.rawText)}</t></is></c>""")
                    outColIdx++
                }
            }
            sb.append("</row>")
        }

        // Optional Summary Formula Row
        if (options.includeSummaryFormulaRow && sheet.rows.isNotEmpty()) {
            val totalRowNum = sheet.rows.size + 2
            val lastDataRowNum = sheet.rows.size + 1
            val totalsBySrcCol = sheet.columnTotals()

            sb.append("""<row r="$totalRowNum" ht="22" customHeight="1">""")
            var outColIdx = 0
            for (srcColIdx in sheet.columns.indices) {
                val colMeta = sheet.columns[srcColIdx]
                val colLetter = excelColLetter(outColIdx)
                val cellRef = "$colLetter$totalRowNum"
                val sumVal = totalsBySrcCol[srcColIdx]

                if (srcColIdx == 0 && sumVal == null) {
                    sb.append("""<c r="$cellRef" s="3" t="inlineStr"><is><t>TOTAL</t></is></c>""")
                    outColIdx++
                } else if (options.splitUnitIntoAdjacentColumn && colMeta.detectedUnit.isNotBlank()) {
                    if (outColIdx in numericExcelColIndices && sumVal != null) {
                        sb.append("""<c r="$cellRef" s="3" t="n"><f>SUM(${colLetter}2:${colLetter}$lastDataRowNum)</f><v>$sumVal</v></c>""")
                    } else {
                        sb.append("""<c r="$cellRef" s="3" t="inlineStr"><is><t></t></is></c>""")
                    }
                    outColIdx++
                    val unitRef = "${excelColLetter(outColIdx)}$totalRowNum"
                    sb.append("""<c r="$unitRef" s="3" t="inlineStr"><is><t>${xmlEscape(colMeta.detectedUnit)}</t></is></c>""")
                    outColIdx++
                } else if (outColIdx in numericExcelColIndices && sumVal != null) {
                    sb.append("""<c r="$cellRef" s="3" t="n"><f>SUM(${colLetter}2:${colLetter}$lastDataRowNum)</f><v>$sumVal</v></c>""")
                    outColIdx++
                } else if (sumVal != null) {
                    val unit = colMeta.detectedUnit
                    val formatted = if (unit == "$" || unit == "€" || unit == "£") {
                        "$unit${formatCleanNumber(sumVal)}"
                    } else {
                        "${formatCleanNumber(sumVal)} $unit".trim()
                    }
                    sb.append("""<c r="$cellRef" s="3" t="inlineStr"><is><t>${xmlEscape(formatted)}</t></is></c>""")
                    outColIdx++
                } else {
                    sb.append("""<c r="$cellRef" s="3" t="inlineStr"><is><t></t></is></c>""")
                    outColIdx++
                }
            }
            sb.append("</row>")
        }

        sb.append("</sheetData>")
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun buildExpandedHeaders(sheet: ScannedSheet, options: ExportOptions): List<String> {
        val headers = mutableListOf<String>()
        for (col in sheet.columns) {
            if (options.splitUnitIntoAdjacentColumn && col.detectedUnit.isNotBlank()) {
                headers.add(col.name)
                headers.add("${col.name} Unit")
            } else {
                headers.add(col.headerWithUnit(options.stripUnitsFromNumbers))
            }
        }
        return headers
    }

    fun getFileProviderUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Shares the exported .xlsx or .csv file directly to WhatsApp (or WhatsApp Business),
     * falling back gracefully to the Android Share Sheet if WhatsApp is not installed on the device.
     */
    fun shareToWhatsApp(
        context: Context,
        file: File,
        format: ExportFormat,
        sheetTitle: String
    ): String {
        val uri = getFileProviderUri(context, file)
        val whatsAppPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
        val pm = context.packageManager

        val installedWhatsApp = whatsAppPackages.firstOrNull { pkg ->
            runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_TEXT,
                "📊 SheetSnap Export: $sheetTitle (${file.name})"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (installedWhatsApp != null) {
            try {
                sendIntent.setPackage(installedWhatsApp)
                context.startActivity(sendIntent)
                "Opening WhatsApp with ${file.name}..."
            } catch (e: ActivityNotFoundException) {
                launchSystemShareChooser(context, sendIntent, "Share ${file.name} via")
                "WhatsApp unavailable; opened share chooser for ${file.name}"
            }
        } else {
            launchSystemShareChooser(context, sendIntent, "Send ${file.name} to WhatsApp or Cloud")
            "Opened share sheet for ${file.name} (WhatsApp not installed on emulator)"
        }
    }

    /**
     * Opens the native Android Share Sheet for Google Drive, Google Sheets, OneDrive, Dropbox, Email, etc.
     */
    fun shareToCloudOrApps(
        context: Context,
        file: File,
        format: ExportFormat,
        sheetTitle: String
    ): String {
        val uri = getFileProviderUri(context, file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, sheetTitle)
            putExtra(
                Intent.EXTRA_TEXT,
                "Extracted table from SheetSnap: $sheetTitle (${file.name})"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launchSystemShareChooser(context, sendIntent, "Export ${file.name} to Cloud / Apps")
        return "Sharing ${file.name} (${file.length() / 1024 + 1} KB)"
    }

    private fun launchSystemShareChooser(context: Context, baseIntent: Intent, title: String) {
        val chooser = Intent.createChooser(baseIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun putZipEntry(zos: ZipOutputStream, name: String, xmlContent: String) {
        val entry = ZipEntry(name)
        zos.putNextEntry(entry)
        zos.write(xmlContent.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()
    }

    private fun excelColLetter(colIndexZeroBased: Int): String {
        var n = colIndexZeroBased
        val sb = StringBuilder()
        while (n >= 0) {
            sb.insert(0, ('A' + (n % 26)))
            n = (n / 26) - 1
        }
        return sb.toString()
    }

    private fun formatCleanNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", value)
        }
    }

    private fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }

    private fun xmlEscape(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
