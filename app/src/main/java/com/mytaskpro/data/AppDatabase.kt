package com.mytaskpro.data


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Task::class, Note::class], version = 4)
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
                // Rename imageUri to imageUris and make it a List<String>
                database.execSQL("ALTER TABLE Note RENAME COLUMN imageUri TO imageUris")
                // Add new column for PDF URI
                database.execSQL("ALTER TABLE Note ADD COLUMN pdfUri TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Rename pdfUri to pdfUris and make it a List<String>
                database.execSQL("ALTER TABLE Note RENAME COLUMN pdfUri TO pdfUris")
                // Update existing pdfUris to be a JSON array if not null
                database.execSQL("UPDATE Note SET pdfUris = '[' || pdfUris || ']' WHERE pdfUris IS NOT NULL")
            }
        }
    }
}