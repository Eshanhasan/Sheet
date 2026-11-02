package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.UnitDetector
import com.example.model.DetectedCell
import com.example.model.ScanMode
import com.example.model.ScannedSheet
import com.example.model.SheetColumn
import org.json.JSONArray

@Entity(tableName = "scanned_sheets")
data class ScannedSheetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val subtitle: String,
    val scanModeName: String,
    val createdAt: Long,
    val headersJson: String,
    val rowsJson: String,
    val notes: String,
    val confidenceScore: Float,
    val paperTemplateId: Int = 0
) {
    fun toDomain(): ScannedSheet {
        val headersArray = JSONArray(headersJson)
        val headerNames = (0 until headersArray.length()).map { headersArray.getString(it) }

        val outerRowsArray = JSONArray(rowsJson)
        val parsedRows: List<List<DetectedCell>> = (0 until outerRowsArray.length()).map { rIdx ->
            val rowArr = outerRowsArray.getJSONArray(rIdx)
            (0 until rowArr.length()).map { cIdx ->
                UnitDetector.parseCell(rowArr.getString(cIdx), confidence = confidenceScore)
            }
        }

        val columns: List<SheetColumn> = headerNames.mapIndexed { colIdx, name ->
            val colCells = parsedRows.mapNotNull { it.getOrNull(colIdx) }
            UnitDetector.inferColumnMetadata(name, colCells)
        }

        val mode = runCatching { ScanMode.valueOf(scanModeName) }.getOrDefault(ScanMode.AUTO_DETECT)

        return ScannedSheet(
            id = id,
            title = title,
            subtitle = subtitle,
            scanMode = mode,
            createdAt = createdAt,
            columns = columns,
            rows = parsedRows,
            notes = notes,
            confidenceScore = confidenceScore,
            paperTemplateId = paperTemplateId
        )
    }
}

fun ScannedSheet.toEntity(): ScannedSheetEntity {
    val headersArray = JSONArray()
    columns.forEach { headersArray.put(it.name) }

    val outerRowsArray = JSONArray()
    rows.forEach { row ->
        val rowArr = JSONArray()
        row.forEach { cell -> rowArr.put(cell.rawText) }
        outerRowsArray.put(rowArr)
    }

    return ScannedSheetEntity(
        id = id,
        title = title,
        subtitle = subtitle,
        scanModeName = scanMode.name,
        createdAt = createdAt,
        headersJson = headersArray.toString(),
        rowsJson = outerRowsArray.toString(),
        notes = notes,
        confidenceScore = confidenceScore,
        paperTemplateId = paperTemplateId
    )
}
