package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.UnitDetector
import com.example.model.DetectedCell
import com.example.model.ExportFormat
import com.example.model.ScannedSheet
import com.example.model.UnitCategory
import com.example.ui.theme.AmberUnitAccent
import com.example.ui.theme.CyanCurrencyAccent
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.RoseAlertAccent
import com.example.ui.theme.VioletWeightAccent
import com.example.ui.theme.WhatsAppGreen
import java.util.Locale

private data class EditingCellCoord(val rowIndex: Int, val colIndex: Int)

@Composable
fun WorkbenchEditorScreen(
    sheet: ScannedSheet?,
    onUpdateTitle: (String) -> Unit,
    onUpdateCell: (Int, Int, String) -> Unit,
    onConvertSingleCell: (Int, Int, UnitDetector.ConversionOption) -> Unit,
    onConvertColumn: (Int, UnitDetector.ConversionOption) -> Unit,
    onUpdateColumnHeader: (Int, String) -> Unit,
    onAddRow: () -> Unit,
    onDeleteRow: (Int) -> Unit,
    onAddColumn: (String, String) -> Unit,
    onDeleteColumn: (Int) -> Unit,
    onOpenExportHub: (ExportFormat) -> Unit,
    onQuickWhatsAppExport: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    if (sheet == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.TableView,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Active Spreadsheet Loaded",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scan a physical paper table or select a saved scan from the Vault to edit cells and numerical units.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToScanner,
                        modifier = Modifier.testTag("empty_workbench_scan_button")
                    ) {
                        Text("Open Paper Scanner")
                    }
                }
            }
        }
        return
    }

    var editingCell by remember { mutableStateOf<EditingCellCoord?>(null) }
    var editingColumnIndex by remember { mutableStateOf<Int?>(null) }
    var showAddColumnDialog by remember { mutableStateOf(false) }
    var showRenameTitleDialog by remember { mutableStateOf(false) }

    val horizontalScrollState = rememberScrollState()
    val columnTotals = remember(sheet.rows, sheet.columns) { sheet.columnTotals() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Header Card: Document Title, Confidence Badge, and Detected Units Strip
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { showRenameTitleDialog = true }
                                .testTag("workbench_sheet_title")
                        ) {
                            Text(
                                text = sheet.title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename sheet",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "${sheet.rowCount} rows × ${sheet.columnCount} cols • ${sheet.numericCellCount} numeric unit cells • ${(sheet.confidenceScore * 100).toInt()}% OCR confidence",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Quick WhatsApp Button
                    Button(
                        onClick = onQuickWhatsAppExport,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppGreen,
                            contentColor = Color(0xFF041C13)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("workbench_whatsapp_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send to WhatsApp",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WhatsApp",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Detected Numerical Units Strip
                val detectedUnits = sheet.detectedUnitsSummary
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Text(
                            text = "DETECTED UNITS:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (detectedUnits.isEmpty()) {
                        item {
                            UnitPillBadge(text = "Pure Numbers / Text", category = UnitCategory.NUMBER)
                        }
                    } else {
                        items(detectedUnits) { badgeText ->
                            val category = when {
                                badgeText.contains("CUR") -> UnitCategory.CURRENCY
                                badgeText.contains("WT") -> UnitCategory.WEIGHT
                                badgeText.contains("VOL") -> UnitCategory.VOLUME
                                badgeText.contains("DIM") -> UnitCategory.LENGTH
                                badgeText.contains("PCT") -> UnitCategory.PERCENTAGE
                                badgeText.contains("TMP") -> UnitCategory.TEMPERATURE
                                else -> UnitCategory.QUANTITY
                            }
                            UnitPillBadge(text = badgeText, category = category)
                        }
                    }
                }
            }
        }

        // 2. Toolbar: Add Row, Add Column, Export .XLSX, Export .CSV
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onAddRow,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_row_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Row", style = MaterialTheme.typography.labelLarge)
            }

            FilledTonalButton(
                onClick = { showAddColumnDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_column_button")
            ) {
                Icon(Icons.Default.ViewColumn, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Col", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = { onOpenExportHub(ExportFormat.CSV) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("workbench_export_csv_button")
            ) {
                Text(".CSV", style = MaterialTheme.typography.labelLarge)
            }

            Button(
                onClick = { onOpenExportHub(ExportFormat.XLSX) },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("workbench_export_xlsx_button")
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export .XLSX", style = MaterialTheme.typography.labelLarge)
            }
        }

        // 3. 2D Editable Spreadsheet Grid (Horizontal + Vertical Scroll)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScrollState)
            ) {
                // Table Column Header Row
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Row Index Gutter Header
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(54.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    sheet.columns.forEachIndexed { colIdx, col ->
                        val colWidth = if (colIdx == 1 && sheet.columns.size >= 4) 190.dp else 142.dp
                        Column(
                            modifier = Modifier
                                .width(colWidth)
                                .height(54.dp)
                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                .clickable { editingColumnIndex = colIdx }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("column_header_$colIdx"),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = col.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (col.detectedUnit.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = categoryAccentColor(col.dominantCategory).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = col.detectedUnit,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = categoryAccentColor(col.dominantCategory),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${col.dominantCategory.displayName} • Tap to convert",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    // Delete Row Action Gutter Header
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Del",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Scrollable Data Rows + Footer Summary Row
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(sheet.rows) { rowIdx, rowCells ->
                        val rowBg = if (rowIdx % 2 == 0) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        }
                        Row(
                            modifier = Modifier.background(rowBg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Row Number Gutter
                            Box(
                                modifier = Modifier
                                    .width(52.dp)
                                    .height(52.dp)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${rowIdx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Cells in Row
                            for (colIdx in sheet.columns.indices) {
                                val cell = rowCells.getOrNull(colIdx) ?: DetectedCell("")
                                val colWidth = if (colIdx == 1 && sheet.columns.size >= 4) 190.dp else 142.dp
                                SpreadsheetGridCell(
                                    cell = cell,
                                    modifier = Modifier
                                        .width(colWidth)
                                        .height(52.dp)
                                        .testTag("grid_cell_${rowIdx}_${colIdx}"),
                                    onClick = {
                                        editingCell = EditingCellCoord(rowIdx, colIdx)
                                    }
                                )
                            }

                            // Delete Row Button
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(52.dp)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = { onDeleteRow(rowIdx) },
                                    enabled = sheet.rows.size > 1,
                                    modifier = Modifier.testTag("delete_row_$rowIdx")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete row ${rowIdx + 1}",
                                        tint = if (sheet.rows.size > 1) {
                                            RoseAlertAccent.copy(alpha = 0.8f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Live Formula Summary Row (Σ TOTAL)
                    item {
                        Row(
                            modifier = Modifier
                                .background(EmeraldPrimary.copy(alpha = 0.14f))
                                .border(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(52.dp)
                                    .height(50.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Functions,
                                    contentDescription = "Formula Sum",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            sheet.columns.forEachIndexed { colIdx, col ->
                                val colWidth = if (colIdx == 1 && sheet.columns.size >= 4) 190.dp else 142.dp
                                val sum = columnTotals[colIdx]
                                Box(
                                    modifier = Modifier
                                        .width(colWidth)
                                        .height(50.dp)
                                        .border(0.5.dp, EmeraldPrimary.copy(alpha = 0.3f))
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (colIdx == 0 && sum == null) {
                                        Text(
                                            text = "Σ TOTAL (SUM)",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = EmeraldPrimary
                                        )
                                    } else if (sum != null) {
                                        val formattedSum = if (sum % 1.0 == 0.0) {
                                            sum.toLong().toString()
                                        } else {
                                            String.format(Locale.US, "%.2f", sum)
                                        }
                                        val displayWithUnit = when {
                                            col.detectedUnit == "$" || col.detectedUnit == "€" || col.detectedUnit == "£" ->
                                                "${col.detectedUnit}$formattedSum"
                                            col.detectedUnit.isNotBlank() ->
                                                "$formattedSum ${col.detectedUnit}"
                                            else -> formattedSum
                                        }
                                        Text(
                                            text = displayWithUnit,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    } else {
                                        Text(
                                            text = "—",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.width(48.dp).height(50.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    // 4. Interactive Cell Edit & Unit Conversion Dialog
    val activeCoord = editingCell
    if (activeCoord != null) {
        val currentCell = sheet.rows
            .getOrNull(activeCoord.rowIndex)
            ?.getOrNull(activeCoord.colIndex) ?: DetectedCell("")
        val colName = sheet.columns.getOrNull(activeCoord.colIndex)?.name ?: "Cell"
        var draftText by remember(activeCoord, currentCell.rawText) {
            mutableStateOf(currentCell.rawText)
        }
        val livePreviewCell = remember(draftText) { UnitDetector.parseCell(draftText) }
        val conversions = remember(currentCell.unitSymbol) {
            UnitDetector.availableConversionsFor(currentCell.unitSymbol)
        }

        AlertDialog(
            onDismissRequest = { editingCell = null },
            title = {
                Text(
                    text = "Edit $colName (Row ${activeCoord.rowIndex + 1})",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { draftText = it },
                        label = { Text("Cell Value & Unit (e.g. $42.50, 15 kg, 24 pcs)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cell_editor_input")
                    )

                    // Live Unit Detection Inspector Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "LIVE NUMERICAL UNIT PARSER",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberUnitAccent
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (livePreviewCell.isNumeric) {
                                    "Number: ${livePreviewCell.cleanNumber}  |  Unit: '${livePreviewCell.unitSymbol.ifBlank { "none" }}' (${livePreviewCell.unitCategory.displayName})"
                                } else {
                                    "Detected as Text / Label"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    if (conversions.isNotEmpty()) {
                        Text(
                            text = "1-Tap Unit Conversion:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            conversions.forEach { option ->
                                OutlinedButton(
                                    onClick = {
                                        onConvertSingleCell(activeCoord.rowIndex, activeCoord.colIndex, option)
                                        editingCell = null
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(option.label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateCell(activeCoord.rowIndex, activeCoord.colIndex, draftText)
                        editingCell = null
                    },
                    modifier = Modifier.testTag("save_cell_edit_button")
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCell = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. Column Header & Batch Unit Converter Dialog
    val activeColIdx = editingColumnIndex
    if (activeColIdx != null && activeColIdx in sheet.columns.indices) {
        val col = sheet.columns[activeColIdx]
        var draftHeader by remember(activeColIdx, col.name) { mutableStateOf(col.name) }
        val conversions = remember(col.detectedUnit) {
            UnitDetector.availableConversionsFor(col.detectedUnit)
        }

        AlertDialog(
            onDismissRequest = { editingColumnIndex = null },
            title = {
                Text("Column '${col.name}' & Unit Settings")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = draftHeader,
                        onValueChange = { draftHeader = it },
                        label = { Text("Column Header Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (conversions.isNotEmpty()) {
                        Text(
                            text = "Batch Convert All '${col.detectedUnit}' Values in Column:",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberUnitAccent
                        )
                        conversions.forEach { option ->
                            FilledTonalButton(
                                onClick = {
                                    onConvertColumn(activeColIdx, option)
                                    editingColumnIndex = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Convert Entire Column: ${option.label}")
                            }
                        }
                    }

                    if (sheet.columns.size > 1) {
                        OutlinedButton(
                            onClick = {
                                onDeleteColumn(activeColIdx)
                                editingColumnIndex = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseAlertAccent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete Column")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateColumnHeader(activeColIdx, draftHeader)
                        editingColumnIndex = null
                    }
                ) {
                    Text("Save Header")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingColumnIndex = null }) {
                    Text("Close")
                }
            }
        )
    }

    // 6. Add Column Dialog
    if (showAddColumnDialog) {
        var newColName by remember { mutableStateOf("") }
        var newColUnit by remember { mutableStateOf("$") }
        AlertDialog(
            onDismissRequest = { showAddColumnDialog = false },
            title = { Text("Add New Column") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newColName,
                        onValueChange = { newColName = it },
                        label = { Text("Column Name (e.g. Tax, Weight, Rate)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newColUnit,
                        onValueChange = { newColUnit = it },
                        label = { Text("Default Unit Symbol (e.g. $, kg, pcs, % or blank)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddColumn(newColName, newColUnit.trim())
                        showAddColumnDialog = false
                    }
                ) {
                    Text("Add Column")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddColumnDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 7. Rename Sheet Title Dialog
    if (showRenameTitleDialog) {
        var draftTitle by remember(sheet.title) { mutableStateOf(sheet.title) }
        AlertDialog(
            onDismissRequest = { showRenameTitleDialog = false },
            title = { Text("Rename Extracted Table") },
            text = {
                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = { draftTitle = it },
                    label = { Text("Spreadsheet Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (draftTitle.isNotBlank()) {
                            onUpdateTitle(draftTitle.trim())
                        }
                        showRenameTitleDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameTitleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SpreadsheetGridCell(
    cell: DetectedCell,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = categoryAccentColor(cell.unitCategory)
    Box(
        modifier = modifier
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (cell.isNumeric && cell.unitSymbol.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = cell.formattedDisplay(stripUnit = true),
                    fontFamily = IbmPlexMonoFontFamily,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accent.copy(alpha = 0.16f),
                    border = BorderStroke(0.5.dp, accent.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = cell.unitSymbol,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        } else {
            Text(
                text = cell.rawText,
                fontFamily = if (cell.isNumeric) IbmPlexMonoFontFamily else MaterialTheme.typography.bodyMedium.fontFamily,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun UnitPillBadge(
    text: String,
    category: UnitCategory,
    modifier: Modifier = Modifier
) {
    val color = categoryAccentColor(category)
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

fun categoryAccentColor(category: UnitCategory): Color {
    return when (category) {
        UnitCategory.CURRENCY -> CyanCurrencyAccent
        UnitCategory.WEIGHT -> AmberUnitAccent
        UnitCategory.VOLUME -> VioletWeightAccent
        UnitCategory.LENGTH -> EmeraldBright
        UnitCategory.QUANTITY -> EmeraldPrimary
        UnitCategory.PERCENTAGE -> AmberUnitAccent
        UnitCategory.TEMPERATURE -> RoseAlertAccent
        UnitCategory.NUMBER -> EmeraldBright
        UnitCategory.TEXT -> Color(0xFF94B8A8)
    }
}
