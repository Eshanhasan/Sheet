package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScannedSheetEntity::class], version = 1, exportSchema = false)
abstract class SheetSnapDatabase : RoomDatabase() {
    abstract fun sheetDao(): SheetDao

    companion object {
        @Volatile
        private var INSTANCE: SheetSnapDatabase? = null

        fun getInstance(context: Context): SheetSnapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SheetSnapDatabase::class.java,
                    "sheetsnap_ledger.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
