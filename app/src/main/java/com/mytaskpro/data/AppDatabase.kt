package com.mytaskpro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Task::class,
        Note::class,
        UserBadgeInfo::class,
        CustomCategory::class
    ],
    version = 13,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun badgeDao(): BadgeDao
    abstract fun customCategoryDao(): CustomCategoryDao

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
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13
                    )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Log successful database open
                            android.util.Log.d("AppDatabase", "Database opened successfully")
                        }

                        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                            super.onDestructiveMigration(db)
                            // Log if destructive migration occurs (shouldn't happen now)
                            android.util.Log.e("AppDatabase", "Destructive migration occurred!")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE notes ADD COLUMN photo_path TEXT")
                    database.execSQL("ALTER TABLE notes ADD COLUMN scanned_text TEXT")
                    android.util.Log.d("AppDatabase", "Migration 1->2 completed successfully")
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Error in migration 1->2: ${e.message}")
                    throw e  // Rethrow to let Room handle the error
                }
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
                try {
                    // Start transaction for complex migration
                    database.beginTransaction()
                    
                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS tasks_temp (
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

                    // Log the number of rows before migration
                    val cursor = database.query("SELECT COUNT(*) FROM tasks")
                    cursor.moveToFirst()
                    val rowCount = cursor.getInt(0)
                    cursor.close()
                    android.util.Log.d("AppDatabase", "Migrating $rowCount tasks")

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

                    // Verify data migration
                    val newCursor = database.query("SELECT COUNT(*) FROM tasks_temp")
                    newCursor.moveToFirst()
                    val newRowCount = newCursor.getInt(0)
                    newCursor.close()
                    
                    if (newRowCount != rowCount) {
                        throw IllegalStateException("Data loss during migration! Original: $rowCount, New: $newRowCount")
                    }

                    database.execSQL("DROP TABLE tasks")
                    database.execSQL("ALTER TABLE tasks_temp RENAME TO tasks")
                    
                    // Mark transaction as successful
                    database.setTransactionSuccessful()
                    android.util.Log.d("AppDatabase", "Migration 8->9 completed successfully")
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Error in migration 8->9: ${e.message}")
                    throw e
                } finally {
                    database.endTransaction()
                }
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the lastModified column to the tasks table
                database.execSQL("ALTER TABLE tasks ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")

                // Update the lastModified column with the current timestamp for existing tasks
                database.execSQL("UPDATE tasks SET lastModified = strftime('%s', 'now') * 1000")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
           override fun migrate(database: SupportSQLiteDatabase) {
             database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_badge_info (
                        userId TEXT PRIMARY KEY NOT NULL,
                        currentBadge TEXT NOT NULL,
                        tasksCompleted INTEGER NOT NULL,
                        streak INTEGER NOT NULL,
                        lastUpdated INTEGER NOT NULL
                    )
                """)
    }
}

private val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add the priority column to the tasks table
        database.execSQL("ALTER TABLE tasks ADD COLUMN priority TEXT NOT NULL DEFAULT 'MEDIUM'")
    }
}

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the custom_categories table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS custom_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        color INTEGER NOT NULL
                    )
                """)
            }
        }
    }  // end of companion object
}  // end of AppDatabase class
