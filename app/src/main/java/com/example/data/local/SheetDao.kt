package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SheetDao {
    @Query("SELECT * FROM scanned_sheets ORDER BY createdAt DESC")
    fun observeAllSheets(): Flow<List<ScannedSheetEntity>>

    @Query("SELECT COUNT(*) FROM scanned_sheets")
    suspend fun getSheetCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSheet(sheet: ScannedSheetEntity): Long

    @Update
    suspend fun updateSheet(sheet: ScannedSheetEntity)

    @Query("DELETE FROM scanned_sheets WHERE id = :id")
    suspend fun deleteSheetById(id: Long)
}
