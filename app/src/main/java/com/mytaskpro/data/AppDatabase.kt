package com.mytaskpro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Task::class, Note::class], version = 9) // Update version to 9
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_4_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Note ADD COLUMN photo_path TEXT")
                database.execSQL("ALTER TABLE Note ADD COLUMN scanned_text TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Note ADD COLUMN imageUri TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Note RENAME COLUMN imageUri TO imageUris")
                database.execSQL("ALTER TABLE Note ADD COLUMN pdfUri TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Note RENAME COLUMN pdfUri TO pdfUris")
                database.execSQL("UPDATE Note SET pdfUris = '[' || pdfUris || ']' WHERE pdfUris IS NOT NULL")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN notifyOnDueDate INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_4_6 = object : Migration(4, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Note RENAME COLUMN pdfUri TO pdfUris")
                database.execSQL("UPDATE Note SET pdfUris = '[' || pdfUris || ']' WHERE pdfUris IS NOT NULL")
                database.execSQL("ALTER TABLE tasks ADD COLUMN notifyOnDueDate INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN repetitiveSettings TEXT")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN completionDate INTEGER")
            }
        }
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create a temporary table with the new schema
                database.execSQL(
                    """
            CREATE TABLE tasks_temp (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                category TEXT NOT NULL,
                dueDate INTEGER NOT NULL,
                reminderTime INTEGER,
                isCompleted INTEGER NOT NULL,
                completionDate INTEGER,
                isSnoozed INTEGER NOT NULL,
                snoozeCount INTEGER NOT NULL,
                showSnoozeOptions INTEGER NOT NULL,
                notifyOnDueDate INTEGER NOT NULL,
                repetitiveSettings TEXT
            )
        """
                )

                // Copy data from the old table to the new table, converting the category
                database.execSQL(
                    """
            INSERT INTO tasks_temp (id, title, description, category, dueDate, reminderTime, isCompleted, completionDate, isSnoozed, snoozeCount, showSnoozeOptions, notifyOnDueDate, repetitiveSettings)
            SELECT id, title, description, 
            CASE 
                WHEN category = '0' THEN '{"type":"WORK","displayName":"Work"}'
                WHEN category = '1' THEN '{"type":"SCHOOL","displayName":"School"}'
                WHEN category = '2' THEN '{"type":"SOCIAL","displayName":"Social"}'
                WHEN category = '3' THEN '{"type":"CRYPTO","displayName":"Crypto"}'
                WHEN category = '4' THEN '{"type":"HEALTH","displayName":"Health"}'
                WHEN category = '5' THEN '{"type":"MINDFULNESS","displayName":"Mindfulness"}'
                WHEN category = '6' THEN '{"type":"INVOICES","displayName":"Invoices"}'
                ELSE '{"type":"CUSTOM","displayName":"Custom"}'
            END,
            dueDate, reminderTime, isCompleted, completionDate, isSnoozed, snoozeCount, showSnoozeOptions, notifyOnDueDate, repetitiveSettings
            FROM tasks
        """
                )

                // Drop the old table
                database.execSQL("DROP TABLE tasks")

                // Rename the new table to the original name
                database.execSQL("ALTER TABLE tasks_temp RENAME TO tasks")
            }
        }
    }
}
