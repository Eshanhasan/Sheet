package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SheetRepository
import com.example.data.local.SheetSnapDatabase
import com.example.domain.PaperBitmapGenerator
import com.example.domain.PhysicalPaperSpec
import com.example.domain.TableExtractionEngine
import com.example.domain.UnitDetector
import com.example.export.SpreadsheetExporter
import com.example.model.DetectedCell
import com.example.model.ExportFormat
import com.example.model.ExportOptions
import com.example.model.ScanMode
import com.example.model.ScannedSheet
import com.example.model.SheetColumn
import com.example.model.UnitCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppDestination(val route: String) {
    VAULT("vault"),
    SCANNER("scanner"),
    WORKBENCH("workbench"),
    EXPORT_HUB("export_hub")
}

data class SheetSnapUiState(
    val currentDestination: AppDestination = AppDestination.VAULT,
    val selectedScanMode: ScanMode = ScanMode.AUTO_DETECT,
    val activeSheet: ScannedSheet? = null,
    val isExtracting: Boolean = false,
    val extractionProgressLabel: String = "",
    val exportOptions: ExportOptions = ExportOptions(),
    val searchQuery: String = "",
    val selectedCategoryFilter: UnitCategory? = null,
    val statusMessage: String? = null,
    val previewPaperSpec: PhysicalPaperSpec? = null,
    val isDarkTheme: Boolean = true
)

class SheetSnapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SheetRepository = SheetRepository(
        SheetSnapDatabase.getInstance(application).sheetDao()
    )

    val allSheets: StateFlow<List<ScannedSheet>> = repository.allSheets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(SheetSnapUiState())
    val uiState: StateFlow<SheetSnapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialScansIfEmpty()
        }
        viewModelScope.launch {
            allSheets.collect { sheets ->
                val currentActive = _uiState.value.activeSheet
                if (currentActive == null && sheets.isNotEmpty()) {
                    _uiState.update { it.copy(activeSheet = sheets.first()) }
                } else if (currentActive != null) {
                    val refreshed = sheets.find { it.id == currentActive.id }
                    if (refreshed != null) {
                        _uiState.update { it.copy(activeSheet = refreshed) }
                    }
                }
            }
        }
    }

    fun navigateTo(destination: AppDestination) {
        _uiState.update { it.copy(currentDestination = destination) }
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun setScanMode(mode: ScanMode) {
        _uiState.update { it.copy(selectedScanMode = mode) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setCategoryFilter(category: UnitCategory?) {
        _uiState.update { it.copy(selectedCategoryFilter = category) }
    }

    fun selectSheetAndOpenWorkbench(sheet: ScannedSheet) {
        _uiState.update {
            it.copy(
                activeSheet = sheet,
                currentDestination = AppDestination.WORKBENCH
            )
        }
    }

    fun selectSheetAndOpenExport(sheet: ScannedSheet, format: ExportFormat? = null) {
        _uiState.update { state ->
            state.copy(
                activeSheet = sheet,
                exportOptions = if (format != null) state.exportOptions.copy(format = format) else state.exportOptions,
                currentDestination = AppDestination.EXPORT_HUB
            )
        }
    }

    fun showPhysicalPaperPreview(spec: PhysicalPaperSpec?) {
        _uiState.update { it.copy(previewPaperSpec = spec) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun postStatusMessage(message: String) {
        _uiState.update { it.copy(statusMessage = message) }
    }

    /**
     * Runs real ML Kit OCR & 2D spatial row/column clustering on a captured/imported Bitmap.
     */
    fun extractTableFromBitmap(
        bitmap: Bitmap,
        sourceTitle: String = "Camera Paper Scan",
        knownSpec: PhysicalPaperSpec? = null
    ) {
        val mode = _uiState.value.selectedScanMode
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExtracting = true,
                    extractionProgressLabel = "Detecting table rows, columns & numerical units...",
                    previewPaperSpec = null
                )
            }
            try {
                val extracted = TableExtractionEngine.extractFromBitmap(
                    bitmap = bitmap,
                    scanMode = mode,
                    sourceTitle = sourceTitle,
                    knownPaperSpec = knownSpec
                )
                val newId = repository.saveSheet(extracted)
                val savedSheet = extracted.copy(id = newId)
                _uiState.update {
                    it.copy(
                        isExtracting = false,
                        extractionProgressLabel = "",
                        activeSheet = savedSheet,
                        currentDestination = AppDestination.WORKBENCH,
                        statusMessage = "Extracted ${savedSheet.rowCount}×${savedSheet.columnCount} table (${savedSheet.numericCellCount} numeric cells)"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExtracting = false,
                        extractionProgressLabel = "",
                        statusMessage = "Extraction error: ${e.localizedMessage ?: "Failed to process image"}"
                    )
                }
            }
        }
    }

    /**
     * Renders a physical paper template into a real Bitmap and runs the OCR + Spatial Grid Extractor on it.
     */
    fun scanSamplePhysicalPaper(spec: PhysicalPaperSpec) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExtracting = true,
                    extractionProgressLabel = "Scanning '${spec.title}' & extracting units...",
                    previewPaperSpec = null
                )
            }
            val paperBitmap = PaperBitmapGenerator.renderPaperBitmap(spec)
            val extracted = TableExtractionEngine.extractFromBitmap(
                bitmap = paperBitmap,
                scanMode = spec.scanMode,
                sourceTitle = spec.title,
                knownPaperSpec = spec
            )
            val newId = repository.saveSheet(extracted)
            val savedSheet = extracted.copy(id = newId)
            _uiState.update {
                it.copy(
                    isExtracting = false,
                    extractionProgressLabel = "",
                    activeSheet = savedSheet,
                    currentDestination = AppDestination.WORKBENCH,
                    statusMessage = "Scanned ${spec.title} • ${savedSheet.detectedUnitsSummary.size} unit types detected"
                )
            }
        }
    }

    fun updateSheetTitle(newTitle: String) {
        val current = _uiState.value.activeSheet ?: return
        val updated = current.copy(title = newTitle)
        persistActiveSheet(updated)
    }

    fun updateCellValue(rowIndex: Int, colIndex: Int, newRawText: String) {
        val current = _uiState.value.activeSheet ?: return
        if (rowIndex !in current.rows.indices || colIndex !in current.columns.indices) return

        val newParsedCell = UnitDetector.parseCell(newRawText, confidence = 1.0f)
        val updatedRows = current.rows.mapIndexed { rIdx, row ->
            if (rIdx == rowIndex) {
                row.mapIndexed { cIdx, existing ->
                    if (cIdx == colIndex) newParsedCell else existing
                }
            } else {
                row
            }
        }
        val updatedColumns = recomputeColumns(current.columns, updatedRows)
        persistActiveSheet(current.copy(columns = updatedColumns, rows = updatedRows))
    }

    fun convertSingleCellUnit(
        rowIndex: Int,
        colIndex: Int,
        option: UnitDetector.ConversionOption
    ) {
        val current = _uiState.value.activeSheet ?: return
        val cell = current.rows.getOrNull(rowIndex)?.getOrNull(colIndex) ?: return
        val converted = UnitDetector.convertCell(cell, option)
        updateCellValue(rowIndex, colIndex, converted.rawText)
        postStatusMessage("Converted ${cell.rawText} → ${converted.rawText}")
    }

    fun convertColumnUnits(
        colIndex: Int,
        option: UnitDetector.ConversionOption
    ) {
        val current = _uiState.value.activeSheet ?: return
        if (colIndex !in current.columns.indices) return

        val updatedRows = current.rows.map { row ->
            row.mapIndexed { cIdx, cell ->
                if (cIdx == colIndex && cell.isNumeric) {
                    UnitDetector.convertCell(cell, option)
                } else {
                    cell
                }
            }
        }
        val updatedColumns = recomputeColumns(current.columns, updatedRows)
        persistActiveSheet(current.copy(columns = updatedColumns, rows = updatedRows))
        postStatusMessage("Converted column '${current.columns[colIndex].name}' to ${option.targetUnit}")
    }

    fun updateColumnHeader(colIndex: Int, newHeaderName: String) {
        val current = _uiState.value.activeSheet ?: return
        if (colIndex !in current.columns.indices) return
        val updatedCols = current.columns.mapIndexed { idx, col ->
            if (idx == colIndex) col.copy(name = newHeaderName.ifBlank { "Col ${idx + 1}" }) else col
        }
        persistActiveSheet(current.copy(columns = updatedCols))
    }

    fun addRow() {
        val current = _uiState.value.activeSheet ?: return
        val newRow = current.columns.mapIndexed { idx, col ->
            when {
                idx == 0 -> UnitDetector.parseCell("Item #${current.rows.size + 1}")
                col.detectedUnit == "$" || col.detectedUnit == "€" || col.detectedUnit == "£" ->
                    UnitDetector.parseCell("${col.detectedUnit}0.00")
                col.detectedUnit.isNotBlank() ->
                    UnitDetector.parseCell("0 ${col.detectedUnit}")
                col.dominantCategory != UnitCategory.TEXT ->
                    UnitDetector.parseCell("0")
                else -> UnitDetector.parseCell("-")
            }
        }
        val updatedRows = current.rows + listOf(newRow)
        persistActiveSheet(
            current.copy(
                rows = updatedRows,
                subtitle = "Edited ${updatedRows.size} rows × ${current.columns.size} cols"
            )
        )
        postStatusMessage("Added Row #${updatedRows.size}")
    }

    fun deleteRow(rowIndex: Int) {
        val current = _uiState.value.activeSheet ?: return
        if (current.rows.size <= 1 || rowIndex !in current.rows.indices) return
        val updatedRows = current.rows.filterIndexed { idx, _ -> idx != rowIndex }
        val updatedCols = recomputeColumns(current.columns, updatedRows)
        persistActiveSheet(current.copy(columns = updatedCols, rows = updatedRows))
        postStatusMessage("Removed Row #${rowIndex + 1}")
    }

    fun addColumn(headerName: String, defaultUnit: String) {
        val current = _uiState.value.activeSheet ?: return
        val cleanHeader = headerName.trim().ifBlank { "Column ${current.columns.size + 1}" }
        val sampleCellText = if (defaultUnit.isBlank()) {
            "0"
        } else if (defaultUnit == "$" || defaultUnit == "€" || defaultUnit == "£") {
            "${defaultUnit}0.00"
        } else {
            "0 $defaultUnit"
        }
        val updatedRows = current.rows.map { row ->
            row + UnitDetector.parseCell(sampleCellText)
        }
        val newColMeta = UnitDetector.inferColumnMetadata(
            cleanHeader,
            updatedRows.map { it.last() }
        )
        val updatedCols = current.columns + newColMeta
        persistActiveSheet(current.copy(columns = updatedCols, rows = updatedRows))
        postStatusMessage("Added column '$cleanHeader'")
    }

    fun deleteColumn(colIndex: Int) {
        val current = _uiState.value.activeSheet ?: return
        if (current.columns.size <= 1 || colIndex !in current.columns.indices) return
        val removedName = current.columns[colIndex].name
        val updatedCols = current.columns.filterIndexed { idx, _ -> idx != colIndex }
        val updatedRows = current.rows.map { row ->
            row.filterIndexed { idx, _ -> idx != colIndex }
        }
        persistActiveSheet(current.copy(columns = updatedCols, rows = updatedRows))
        postStatusMessage("Deleted column '$removedName'")
    }

    fun deleteSheet(sheetId: Long) {
        viewModelScope.launch {
            repository.deleteSheet(sheetId)
            postStatusMessage("Deleted scan from vault")
        }
    }

    fun updateExportOptions(transform: (ExportOptions) -> ExportOptions) {
        _uiState.update { it.copy(exportOptions = transform(it.exportOptions)) }
    }

    fun saveActiveSheetToCloudUri(uri: Uri) {
        val sheet = _uiState.value.activeSheet ?: return
        val options = _uiState.value.exportOptions
        viewModelScope.launch {
            val result = SpreadsheetExporter.writeSheetToUri(
                context = getApplication(),
                uri = uri,
                sheet = sheet,
                options = options
            )
            result.onSuccess { fileName ->
                postStatusMessage("Saved $fileName to Cloud / Device storage")
            }.onFailure { err ->
                postStatusMessage("Save failed: ${err.localizedMessage}")
            }
        }
    }

    fun exportAndShareToWhatsApp(sheet: ScannedSheet? = _uiState.value.activeSheet, forcedFormat: ExportFormat? = null) {
        val targetSheet = sheet ?: return
        val options = if (forcedFormat != null) {
            _uiState.value.exportOptions.copy(format = forcedFormat)
        } else {
            _uiState.value.exportOptions
        }
        viewModelScope.launch {
            runCatching {
                val context = getApplication<Application>()
                val file = SpreadsheetExporter.exportToCacheFile(context, targetSheet, options)
                SpreadsheetExporter.shareToWhatsApp(context, file, options.format, targetSheet.title)
            }.onSuccess { msg ->
                postStatusMessage(msg)
            }.onFailure { e ->
                postStatusMessage("WhatsApp export error: ${e.localizedMessage}")
            }
        }
    }

    fun exportAndShareToCloudOrApps(sheet: ScannedSheet? = _uiState.value.activeSheet, forcedFormat: ExportFormat? = null) {
        val targetSheet = sheet ?: return
        val options = if (forcedFormat != null) {
            _uiState.value.exportOptions.copy(format = forcedFormat)
        } else {
            _uiState.value.exportOptions
        }
        viewModelScope.launch {
            runCatching {
                val context = getApplication<Application>()
                val file = SpreadsheetExporter.exportToCacheFile(context, targetSheet, options)
                SpreadsheetExporter.shareToCloudOrApps(context, file, options.format, targetSheet.title)
            }.onSuccess { msg ->
                postStatusMessage(msg)
            }.onFailure { e ->
                postStatusMessage("Export failed: ${e.localizedMessage}")
            }
        }
    }

    private fun recomputeColumns(
        existingCols: List<SheetColumn>,
        rows: List<List<DetectedCell>>
    ): List<SheetColumn> {
        return existingCols.mapIndexed { colIdx, col ->
            val colCells = rows.mapNotNull { it.getOrNull(colIdx) }
            UnitDetector.inferColumnMetadata(col.name, colCells)
        }
    }

    private fun persistActiveSheet(updated: ScannedSheet) {
        _uiState.update { it.copy(activeSheet = updated) }
        viewModelScope.launch {
            if (updated.id == 0L) {
                val id = repository.saveSheet(updated)
                _uiState.update { it.copy(activeSheet = updated.copy(id = id)) }
            } else {
                repository.updateSheet(updated)
            }
        }
    }
}
