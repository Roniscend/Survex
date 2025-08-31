package com.example.survey.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SessionEntity::class, MediaEntity::class],
    version = 3,
    exportSchema = false
)
abstract class SurveyDatabase : RoomDatabase() {

    abstract fun surveyDao(): SurveyDao

    companion object {
        @Volatile
        private var INSTANCE: SurveyDatabase? = null

        // Migration from version 2 to 3
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d("SurveyDatabase", "Migrating from version 2 to 3")
                // No schema changes needed - just version bump
                // Add any SQL migration commands here if needed in the future
            }
        }

        // ADDED: Migration from version 1 to 2 (in case users still have version 1)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d("SurveyDatabase", "Migrating from version 1 to 2")
                // No schema changes needed - just version bump
            }
        }

        fun getDatabase(context: Context): SurveyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SurveyDatabase::class.java,
                    "survey_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)  // ADDED: Both migrations
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
