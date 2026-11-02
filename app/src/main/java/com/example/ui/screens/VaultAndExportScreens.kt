package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.PaperBitmapGenerator
import com.example.domain.PhysicalPaperSpec
import com.example.export.SpreadsheetExporter
import com.example.model.ExportFormat
import com.example.model.ExportOptions
import com.example.model.ScannedSheet
import com.example.model.UnitCategory
import com.example.ui.theme.AmberUnitAccent
import com.example.ui.theme.CloudBlue
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestCard
import com.example.ui.theme.ForestObsidian
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.IvoryText
import com.example.ui.theme.RoseAlertAccent
import com.example.ui.theme.WhatsAppGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScanVaultScreen(
    sheets: List<ScannedSheet>,
    searchQuery: String,
    selectedCategoryFilter: UnitCategory?,
    isDarkTheme: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onCategoryFilterChange: (UnitCategory?) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenScanner: () -> Unit,
    onScanSamplePaper: (PhysicalPaperSpec) -> Unit,
    onOpenSheetInWorkbench: (ScannedSheet) -> Unit,
    onOpenSheetInExport: (ScannedSheet, ExportFormat) -> Unit,
    onQuickWhatsAppShare: (ScannedSheet) -> Unit,
    onDeleteSheet: (Long) -> Unit
) {
    val filteredSheets = remember(sheets, searchQuery, selectedCategoryFilter) {
        sheets.filter { sheet ->
            val matchesSearch = searchQuery.isBlank() ||
                sheet.title.contains(searchQuery, ignoreCase = true) ||
                sheet.subtitle.contains(searchQuery, ignoreCase = true) ||
                sheet.rows.flatten().any { it.rawText.contains(searchQuery, ignoreCase = true) }

            val matchesCategory = selectedCategoryFilter == null ||
                sheet.columns.any { it.dominantCategory == selectedCategoryFilter } ||
                sheet.rows.flatten().any { it.unitCategory == selectedCategoryFilter }

            matchesSearch && matchesCategory
        }
    }

    val totalExtractedRows = remember(sheets) { sheets.sumOf { it.rowCount } }
    val totalNumericCells = remember(sheets) { sheets.sumOf { it.numericCellCount } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top App Header & Hero Scanner Banner
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "SheetSnap",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Paper Tables & Numerical Units → .XLSX / .CSV",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Dark or Light Ledger Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hero Card with Generated Illustration
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_scanner_banner_1793622529184),
                            contentDescription = "Physical paper invoice scanning into editable spreadsheet cells",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(205.dp)
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            ForestObsidian.copy(alpha = 0.35f),
                                            ForestObsidian.copy(alpha = 0.85f),
                                            ForestObsidian.copy(alpha = 0.96f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .padding(18.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = EmeraldPrimary.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, EmeraldBright)
                                ) {
                                    Text(
                                        text = "${sheets.size} SCANS • $totalExtractedRows ROWS • $totalNumericCells UNIT CELLS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldBright,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Snap Physical Paper into Editable Spreadsheets",
                                style = MaterialTheme.typography.titleLarge,
                                color = IvoryText
                            )
                            Text(
                                text = "Extracts rows, columns & numerical units ($, kg, mL, pcs, %) into .xlsx or .csv for Cloud Storage & WhatsApp.",
                                style = MaterialTheme.typography.bodySmall,
                                color = IvoryText.copy(alpha = 0.82f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = onOpenScanner,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = Color(0xFF041C13)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("hero_scan_paper_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Scan Physical Table or Itemized List",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Instant Physical Paper Test Carousel
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INSTANT PHYSICAL PAPER TEST SHEETS",
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberUnitAccent
                    )
                    Text(
                        text = "Tap to run OCR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(PaperBitmapGenerator.samplePapers, key = { it.id }) { spec ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .width(220.dp)
                                .clickable { onScanSamplePaper(spec) }
                                .testTag("vault_sample_paper_${spec.id}")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = EmeraldPrimary.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = spec.badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.GridOn,
                                        contentDescription = null,
                                        tint = AmberUnitAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = spec.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${spec.rows.size} rows × ${spec.headers.size} cols",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Search & Numerical Unit Filter Bar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search scanned tables, items, or units ($, kg, mL)...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                val filterCategories = listOf(
                    null to "All Units",
                    UnitCategory.CURRENCY to "Currency ($/€)",
                    UnitCategory.WEIGHT to "Weight (kg/g)",
                    UnitCategory.VOLUME to "Volume (L/mL)",
                    UnitCategory.LENGTH to "Dimension (m/cm)",
                    UnitCategory.QUANTITY to "Count (pcs/box)"
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filterCategories) { (category, label) ->
                        FilterChip(
                            selected = selectedCategoryFilter == category,
                            onClick = { onCategoryFilterChange(category) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
        }

        // 4. Saved Scanned Sheets List
        if (filteredSheets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No matching spreadsheets found",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Try clearing your unit filter or scan a new physical paper table.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredSheets, key = { it.id }) { sheet ->
                ScannedSheetVaultCard(
                    sheet = sheet,
                    onEditClick = { onOpenSheetInWorkbench(sheet) },
                    onExportXlsxClick = { onOpenSheetInExport(sheet, ExportFormat.XLSX) },
                    onExportCsvClick = { onOpenSheetInExport(sheet, ExportFormat.CSV) },
                    onWhatsAppClick = { onQuickWhatsAppShare(sheet) },
                    onDeleteClick = { onDeleteSheet(sheet.id) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ScannedSheetVaultCard(
    sheet: ScannedSheet,
    onEditClick: () -> Unit,
    onExportXlsxClick: () -> Unit,
    onExportCsvClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM d • HH:mm", Locale.US) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick)
            .testTag("sheet_vault_card_${sheet.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldPrimary.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "${sheet.rowCount}×${sheet.columnCount} GRID",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = dateFormat.format(Date(sheet.createdAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = sheet.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = sheet.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete sheet",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Detected Numerical Units Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(sheet.detectedUnitsSummary) { unitBadge ->
                    val cat = when {
                        unitBadge.contains("CUR") -> UnitCategory.CURRENCY
                        unitBadge.contains("WT") -> UnitCategory.WEIGHT
                        unitBadge.contains("VOL") -> UnitCategory.VOLUME
                        unitBadge.contains("DIM") -> UnitCategory.LENGTH
                        else -> UnitCategory.QUANTITY
                    }
                    UnitPillBadge(text = unitBadge, category = cat)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mini 2-Row Tabular Preview
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        sheet.columns.take(4).forEach { col ->
                            Text(
                                text = col.headerWithUnit(true),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    sheet.rows.take(2).forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            row.take(4).forEach { cell ->
                                Text(
                                    text = cell.rawText,
                                    fontFamily = IbmPlexMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Bar: Edit Grid | .XLSX | .CSV | WhatsApp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEditClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onExportXlsxClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(".XLSX", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onExportCsvClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(".CSV", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = onWhatsAppClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color(0xFF041C13)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send to WhatsApp", modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun ExportHubScreen(
    sheet: ScannedSheet?,
    options: ExportOptions,
    onUpdateOptions: ((ExportOptions) -> ExportOptions) -> Unit,
    onSaveToCloudUri: (Uri) -> Unit,
    onShareToWhatsApp: () -> Unit,
    onShareToCloudOrApps: () -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    if (sheet == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select or scan a table first to export.")
        }
        return
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(options.format.mimeType)
    ) { uri: Uri? ->
        if (uri != null) {
            onSaveToCloudUri(uri)
        }
    }

    val fileName = remember(sheet.title, options.format) {
        SpreadsheetExporter.sanitizeFileName(sheet.title, options.format)
    }

    val livePreviewText = remember(sheet, options) {
        SpreadsheetExporter.buildCsvString(sheet, options)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Export Header Summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "EXPORT & CLOUD / WHATSAPP HUB",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = fileName,
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = IbmPlexMonoFontFamily
                            )
                            Text(
                                text = "${sheet.rowCount} rows × ${sheet.columnCount} columns • ${sheet.numericCellCount} numeric unit cells",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Ready for export",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // 2. Format Selection (.XLSX vs .CSV)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. CHOOSE SPREADSHEET FORMAT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExportFormat.entries.forEach { fmt ->
                        val selected = options.format == fmt
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) {
                                EmeraldPrimary.copy(alpha = 0.18f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdateOptions { it.copy(format = fmt) } }
                                .testTag("format_selector_${fmt.extension}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = ".${fmt.extension.uppercase()}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = fmt.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Numerical Unit & Formula Export Controls
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "2. NUMERICAL UNIT & FORMULA OPTIONS",
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberUnitAccent
                    )

                    ExportToggleRow(
                        title = "Clean Numbers + Unit in Column Header",
                        subtitle = "Writes pure numeric cells (e.g. 42.5) and moves unit to header 'Mass (kg)' for instant Excel formulas",
                        checked = options.stripUnitsFromNumbers,
                        onCheckedChange = { checked ->
                            onUpdateOptions { it.copy(stripUnitsFromNumbers = checked) }
                        },
                        testTag = "toggle_strip_units"
                    )

                    ExportToggleRow(
                        title = "Split Units into Dedicated Adjacent Column",
                        subtitle = "Creates a companion '[Col] Unit' column next to every unit-bearing column",
                        checked = options.splitUnitIntoAdjacentColumn,
                        onCheckedChange = { checked ->
                            onUpdateOptions { it.copy(splitUnitIntoAdjacentColumn = checked) }
                        },
                        testTag = "toggle_split_unit_column"
                    )

                    ExportToggleRow(
                        title = "Include SUM() Formula Total Row",
                        subtitle = "Appends a calculated TOTAL footer row with native Excel SUM(C2:Cn) formulas",
                        checked = options.includeSummaryFormulaRow,
                        onCheckedChange = { checked ->
                            onUpdateOptions { it.copy(includeSummaryFormulaRow = checked) }
                        },
                        testTag = "toggle_formula_row"
                    )
                }
            }
        }

        // 4. Direct Destination Actions: WhatsApp, Cloud Storage (SAF), and Share Sheet
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "3. INSTANT EXPORT DESTINATIONS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Direct WhatsApp Export Button
                Button(
                    onClick = onShareToWhatsApp,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color(0xFF041C13)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("export_whatsapp_button")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Send $fileName Directly to WhatsApp",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // Direct Cloud Storage Picker (Google Drive / OneDrive / Dropbox / Device Files)
                Button(
                    onClick = { createDocumentLauncher.launch(fileName) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CloudBlue,
                        contentColor = Color(0xFF031926)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("export_cloud_save_button")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Save $fileName to Cloud Storage / Drive",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // System Share Sheet (Google Sheets, Email, Telegram, etc.)
                OutlinedButton(
                    onClick = onShareToCloudOrApps,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("export_share_sheet_button")
                ) {
                    Icon(Icons.Default.IosShare, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open in Google Sheets / Share to Other Apps",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // 5. Live Output Preview Box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "LIVE GENERATED STRUCTURE PREVIEW ($fileName)",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = livePreviewText.trimEnd(),
                        fontFamily = IbmPlexMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
