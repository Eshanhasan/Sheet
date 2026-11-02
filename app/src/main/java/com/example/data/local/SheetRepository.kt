package com.example.data.local

import com.example.domain.PaperBitmapGenerator
import com.example.model.ScannedSheet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SheetRepository(private val sheetDao: SheetDao) {

    val allSheets: Flow<List<ScannedSheet>> = sheetDao.observeAllSheets()
        .map { entities -> entities.map { it.toDomain() } }

    suspend fun saveSheet(sheet: ScannedSheet): Long {
        return sheetDao.insertSheet(sheet.toEntity())
    }

    suspend fun updateSheet(sheet: ScannedSheet) {
        sheetDao.updateSheet(sheet.toEntity())
    }

    suspend fun deleteSheet(id: Long) {
        sheetDao.deleteSheetById(id)
    }

    suspend fun seedInitialScansIfEmpty() {
        if (sheetDao.getSheetCount() == 0) {
            val now = System.currentTimeMillis()
            PaperBitmapGenerator.samplePapers.take(3).forEachIndexed { index, spec ->
                val domainSheet = PaperBitmapGenerator.buildSheetFromSpec(spec).copy(
                    createdAt = now - (index * 3_600_000L)
                )
                sheetDao.insertSheet(domainSheet.toEntity())
            }
        }
    }
}
