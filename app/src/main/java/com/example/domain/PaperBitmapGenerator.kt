package com.example.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.model.ScanMode
import com.example.model.ScannedSheet

data class PhysicalPaperSpec(
    val id: Int,
    val title: String,
    val subtitle: String,
    val badge: String,
    val scanMode: ScanMode,
    val headers: List<String>,
    val rows: List<List<String>>,
    val footerNote: String
)

object PaperBitmapGenerator {

    val samplePapers: List<PhysicalPaperSpec> = listOf(
        PhysicalPaperSpec(
            id = 1,
            title = "Global Solutions Invoice #45192",
            subtitle = "Wholesale Hardware & Fasteners Table",
            badge = "5-Col Invoice Grid",
            scanMode = ScanMode.STRUCTURED_TABLE,
            headers = listOf("SKU", "Item Description", "Qty", "Unit Price", "Line Total"),
            rows = listOf(
                listOf("M8-440", "Stainless Hex Bolt Kit", "24 pcs", "$18.50", "$444.00"),
                listOf("AL-209", "Anodized Extrusion Rail", "12.5 m", "$32.00", "$400.00"),
                listOf("SL-104", "Industrial Epoxy Resin", "15.0 kg", "$24.80", "$372.00"),
                listOf("铜-882", "Braided Copper Grounding", "45.0 m", "$9.40", "$423.00"),
                listOf("PK-310", "Neoprene Gasket Pack", "8 box", "$41.25", "$330.00"),
                listOf(" LB-901", "Dielectric Grease Drum", "18.5 L", "$16.00", "$296.00")
            ),
            footerNote = "PRINTED LEDGER • NET 30 TERMS • TAX RATE 8.25%"
        ),
        PhysicalPaperSpec(
            id = 2,
            title = "Botanical Lab Batch Ledger L-405",
            subtitle = "Analytical Extraction & Mass Log",
            badge = "Scientific Units",
            scanMode = ScanMode.STRUCTURED_TABLE,
            headers = listOf("Sample ID", "Compound", "Dry Mass", "Solvent Vol", "Yield", "Temp"),
            rows = listOf(
                listOf("L-405A", "Caffea Arabica Ext", "420 g", "1250 mL", "94.2%", "65.0 °C"),
                listOf("L-405B", "Camellia Polyphenol", "315 g", "980 mL", "91.8%", "62.5 °C"),
                listOf("L-405C", "Curcuma Liposome", "1.45 kg", "3.20 L", "96.4%", "68.0 °C"),
                listOf("L-405D", "Rosmarinus Terpene", "280 g", "750 mL", "89.5%", "60.2 °C"),
                listOf("L-405E", "Zingiber Oleoresin", "640 g", "1800 mL", "93.1%", "64.8 °C")
            ),
            footerNote = "QC VERIFIED PAPER LOG • CALIBRATION SCALE ±0.05 g"
        ),
        PhysicalPaperSpec(
            id = 3,
            title = "Artisan Roastery & Provision Receipt",
            subtitle = "Thermal Itemized Paper Receipt",
            badge = "Itemized List",
            scanMode = ScanMode.ITEMIZED_LIST,
            headers = listOf("Item", "Net Weight", "Count", "Unit Rate", "Amount"),
            rows = listOf(
                listOf("Ethiopian Yirgacheffe", "500 g", "3 pk", "$22.00", "$66.00"),
                listOf("Colombian Huila Reserve", "1.20 kg", "2 bag", "$38.50", "$77.00"),
                listOf("Oat Barista Cartons", "12.0 L", "12 pcs", "$4.25", "$51.00"),
                listOf("Raw Demerara Sugar", "2.50 kg", "1 bag", "$14.80", "$14.80"),
                listOf("Unbleached V60 Filters", "180 g", "4 box", "$8.50", "$34.00")
            ),
            footerNote = "THERMAL REGISTER #04 • PAID VISA • THANK YOU"
        ),
        PhysicalPaperSpec(
            id = 4,
            title = "Site Field Material Tally Sheet",
            subtitle = "Clipboard Structural Delivery Check",
            badge = "Metric & Imperial",
            scanMode = ScanMode.AUTO_DETECT,
            headers = listOf("Material", "Length", "Mass", "Bundles", "Est Cost"),
            rows = listOf(
                listOf("Rebar Grade 60 #5", "180 m", "285 kg", "6 pcs", "$684.00"),
                listOf("Galvanized Conduit", "95 m", "112 kg", "10 pcs", "$410.00"),
                listOf("Portland Cement Type I", "40 cm", "450 kg", "9 bag", "$297.00"),
                listOf("Structural Pine Beam", "48 ft", "165 lb", "8 pcs", "$528.00"),
                listOf("Silica Aggregate Sand", "85 cm", "920 kg", "4 ton", "$615.00")
            ),
            footerNote = "SITE GATE #2 PAPER TALLY • INSPECTOR SIGNED"
        )
    )

    fun renderPaperBitmap(spec: PhysicalPaperSpec): Bitmap {
        val width = 1280
        val height = 1040
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Desk backdrop
        canvas.drawColor(Color.parseColor("#11221C"))

        // 2. Physical Paper Sheet with subtle shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#66000000")
        }
        val paperRect = RectF(56f, 44f, width - 56f, height - 44f)
        canvas.drawRoundRect(
            RectF(paperRect.left + 12f, paperRect.top + 16f, paperRect.right + 12f, paperRect.bottom + 16f),
            12f,
            12f,
            shadowPaint
        )

        val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FCFBF7")
        }
        canvas.drawRoundRect(paperRect, 10f, 10f, paperPaint)

        // Subtle horizontal ruled ledger lines
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E6ECE8")
            strokeWidth = 1.5f
        }
        var ruleY = paperRect.top + 180f
        while (ruleY < paperRect.bottom - 80f) {
            canvas.drawLine(paperRect.left + 40f, ruleY, paperRect.right - 40f, ruleY, rulePaint)
            ruleY += 86f
        }

        // 3. Document Header & Ink Stamp
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F1F18")
            textSize = 36f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3E5C50")
            textSize = 23f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        canvas.drawText(spec.title.uppercase(), paperRect.left + 52f, paperRect.top + 86f, titlePaint)
        canvas.drawText(spec.subtitle, paperRect.left + 52f, paperRect.top + 126f, subPaint)

        // Stamp badge on top right
        val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#059669")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val stampRect = RectF(paperRect.right - 310f, paperRect.top + 52f, paperRect.right - 52f, paperRect.top + 114f)
        canvas.drawRoundRect(stampRect, 8f, 8f, stampPaint)
        val stampTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#059669")
            textSize = 22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(spec.badge.uppercase(), stampRect.centerX(), stampRect.centerY() + 8f, stampTextPaint)

        // 4. Table Grid & Columns
        val tableTop = paperRect.top + 175f
        val tableLeft = paperRect.left + 48f
        val tableRight = paperRect.right - 48f
        val tableWidth = tableRight - tableLeft
        val colCount = spec.headers.size
        val rowHeight = 92f

        // Compute column X offsets (give column 1 extra width if it's a description)
        val colWeights = FloatArray(colCount) { idx ->
            if (idx == 1 && colCount >= 4) 1.65f else if (idx == 0 && colCount == 5) 1.25f else 1.0f
        }
        val weightSum = colWeights.sum()
        val colStarts = FloatArray(colCount + 1)
        colStarts[0] = tableLeft
        for (i in 0 until colCount) {
            colStarts[i + 1] = colStarts[i] + (colWeights[i] / weightSum) * tableWidth
        }

        // Header background bar
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#122C23")
        }
        canvas.drawRect(tableLeft, tableTop, tableRight, tableTop + rowHeight, headerBgPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F0FDF4")
            textSize = 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        for (c in 0 until colCount) {
            canvas.drawText(
                spec.headers[c],
                colStarts[c] + 18f,
                tableTop + 56f,
                headerTextPaint
            )
        }

        // Draw rows
        val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10211B")
            textSize = 25f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94B8A8")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            if (spec.scanMode == ScanMode.ITEMIZED_LIST) {
                pathEffect = DashPathEffect(floatArrayOf(10f, 8f), 0f)
            }
        }

        spec.rows.forEachIndexed { rIdx, rowCells ->
            val rowTop = tableTop + (rIdx + 1) * rowHeight
            if (rIdx % 2 == 1) {
                val zebraPaint = Paint().apply { color = Color.parseColor("#F1F6F3") }
                canvas.drawRect(tableLeft, rowTop, tableRight, rowTop + rowHeight, zebraPaint)
            }
            canvas.drawLine(tableLeft, rowTop + rowHeight, tableRight, rowTop + rowHeight, borderPaint)

            for (c in 0 until minOf(colCount, rowCells.size)) {
                canvas.drawText(
                    rowCells[c],
                    colStarts[c] + 18f,
                    rowTop + 56f,
                    cellTextPaint
                )
            }
        }

        // Outer table border & vertical column dividers
        val totalTableBottom = tableTop + (spec.rows.size + 1) * rowHeight
        canvas.drawRect(tableLeft, tableTop, tableRight, totalTableBottom, borderPaint)
        if (spec.scanMode != ScanMode.ITEMIZED_LIST) {
            for (c in 1 until colCount) {
                canvas.drawLine(colStarts[c], tableTop, colStarts[c], totalTableBottom, borderPaint)
            }
        }

        // Footer Note
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B6B5E")
            textSize = 21f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText(spec.footerNote, tableLeft, paperRect.bottom - 38f, footerPaint)

        return bitmap
    }

    fun buildSheetFromSpec(spec: PhysicalPaperSpec): ScannedSheet {
        val parsedRows = spec.rows.map { row ->
            row.map { cellStr -> UnitDetector.parseCell(cellStr, confidence = 0.98f) }
        }
        val columns = spec.headers.mapIndexed { colIdx, headerName ->
            val colCells = parsedRows.mapNotNull { it.getOrNull(colIdx) }
            UnitDetector.inferColumnMetadata(headerName, colCells)
        }
        return ScannedSheet(
            title = spec.title,
            subtitle = spec.subtitle,
            scanMode = spec.scanMode,
            columns = columns,
            rows = parsedRows,
            notes = spec.footerNote,
            confidenceScore = 0.98f,
            paperTemplateId = spec.id
        )
    }
}
