package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Script::class, Folder::class], version = 1, exportSchema = true)
abstract class ScriptDatabase : RoomDatabase() {
    abstract val scriptDao: ScriptDao

    companion object {
        @Volatile
        private var INSTANCE: ScriptDatabase? = null

        fun getDatabase(context: Context): ScriptDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScriptDatabase::class.java,
                    "cueflow_database",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
