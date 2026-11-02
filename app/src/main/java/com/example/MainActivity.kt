package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppDestination
import com.example.ui.SheetSnapViewModel
import com.example.ui.screens.CameraScannerScreen
import com.example.ui.screens.ExportHubScreen
import com.example.ui.screens.ScanVaultScreen
import com.example.ui.screens.WorkbenchEditorScreen
import com.example.ui.theme.SheetSnapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SheetSnapViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            SheetSnapTheme(darkTheme = uiState.isDarkTheme) {
                SheetSnapApp(viewModel = viewModel)
            }
        }
    }
}

private data class NavItemSpec(
    val destination: AppDestination,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun SheetSnapApp(viewModel: SheetSnapViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allSheets by viewModel.allSheets.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val navItems = remember {
        listOf(
            NavItemSpec(
                destination = AppDestination.VAULT,
                label = "Vault",
                selectedIcon = Icons.Filled.FolderSpecial,
                unselectedIcon = Icons.Outlined.FolderSpecial,
                testTag = "nav_tab_vault"
            ),
            NavItemSpec(
                destination = AppDestination.SCANNER,
                label = "Scanner",
                selectedIcon = Icons.Filled.CameraAlt,
                unselectedIcon = Icons.Outlined.CameraAlt,
                testTag = "nav_tab_scanner"
            ),
            NavItemSpec(
                destination = AppDestination.WORKBENCH,
                label = "Workbench",
                selectedIcon = Icons.Filled.TableChart,
                unselectedIcon = Icons.Outlined.TableChart,
                testTag = "nav_tab_workbench"
            ),
            NavItemSpec(
                destination = AppDestination.EXPORT_HUB,
                label = "Export",
                selectedIcon = Icons.Filled.IosShare,
                unselectedIcon = Icons.Outlined.IosShare,
                testTag = "nav_tab_export"
            )
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar {
                        navItems.forEach { item ->
                            val selected = uiState.currentDestination == item.destination
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(item.destination) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail {
                        navItems.forEach { item ->
                            val selected = uiState.currentDestination == item.destination
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(item.destination) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (uiState.currentDestination) {
                        AppDestination.VAULT -> {
                            ScanVaultScreen(
                                sheets = allSheets,
                                searchQuery = uiState.searchQuery,
                                selectedCategoryFilter = uiState.selectedCategoryFilter,
                                isDarkTheme = uiState.isDarkTheme,
                                onSearchQueryChange = viewModel::setSearchQuery,
                                onCategoryFilterChange = viewModel::setCategoryFilter,
                                onToggleTheme = viewModel::toggleTheme,
                                onOpenScanner = { viewModel.navigateTo(AppDestination.SCANNER) },
                                onScanSamplePaper = viewModel::scanSamplePhysicalPaper,
                                onOpenSheetInWorkbench = viewModel::selectSheetAndOpenWorkbench,
                                onOpenSheetInExport = { sheet, format ->
                                    viewModel.selectSheetAndOpenExport(sheet, format)
                                },
                                onQuickWhatsAppShare = { sheet ->
                                    viewModel.exportAndShareToWhatsApp(sheet)
                                },
                                onDeleteSheet = viewModel::deleteSheet
                            )
                        }

                        AppDestination.SCANNER -> {
                            CameraScannerScreen(
                                selectedScanMode = uiState.selectedScanMode,
                                isExtracting = uiState.isExtracting,
                                extractionProgressLabel = uiState.extractionProgressLabel,
                                previewPaperSpec = uiState.previewPaperSpec,
                                onScanModeChange = viewModel::setScanMode,
                                onBitmapCaptured = { bmp, title ->
                                    viewModel.extractTableFromBitmap(bmp, title)
                                },
                                onScanSamplePaper = viewModel::scanSamplePhysicalPaper,
                                onPreviewPaperSpec = viewModel::showPhysicalPaperPreview,
                                onNavigateBack = { viewModel.navigateTo(AppDestination.VAULT) }
                            )
                        }

                        AppDestination.WORKBENCH -> {
                            WorkbenchEditorScreen(
                                sheet = uiState.activeSheet,
                                onUpdateTitle = viewModel::updateSheetTitle,
                                onUpdateCell = viewModel::updateCellValue,
                                onConvertSingleCell = viewModel::convertSingleCellUnit,
                                onConvertColumn = viewModel::convertColumnUnits,
                                onUpdateColumnHeader = viewModel::updateColumnHeader,
                                onAddRow = viewModel::addRow,
                                onDeleteRow = viewModel::deleteRow,
                                onAddColumn = viewModel::addColumn,
                                onDeleteColumn = viewModel::deleteColumn,
                                onOpenExportHub = { format ->
                                    val active = uiState.activeSheet
                                    if (active != null) {
                                        viewModel.selectSheetAndOpenExport(active, format)
                                    }
                                },
                                onQuickWhatsAppExport = {
                                    viewModel.exportAndShareToWhatsApp()
                                },
                                onNavigateToScanner = { viewModel.navigateTo(AppDestination.SCANNER) },
                                onNavigateBack = { viewModel.navigateTo(AppDestination.VAULT) }
                            )
                        }

                        AppDestination.EXPORT_HUB -> {
                            ExportHubScreen(
                                sheet = uiState.activeSheet,
                                options = uiState.exportOptions,
                                onUpdateOptions = viewModel::updateExportOptions,
                                onSaveToCloudUri = viewModel::saveActiveSheetToCloudUri,
                                onShareToWhatsApp = { viewModel.exportAndShareToWhatsApp() },
                                onShareToCloudOrApps = { viewModel.exportAndShareToCloudOrApps() },
                                onNavigateBack = { viewModel.navigateTo(AppDestination.WORKBENCH) }
                            )
                        }
                    }
                }
            }
        }
    }
}
