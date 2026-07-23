package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Medication::class, Schedule::class, IntakeLog::class, UserAccount::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `users` (
                        `email` TEXT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `authProvider` TEXT NOT NULL, 
                        `avatarUrl` TEXT NOT NULL, 
                        `passwordHash` TEXT NOT NULL, 
                        `createdAt` INTEGER NOT NULL, 
                        `lastLoginAt` INTEGER NOT NULL, 
                        PRIMARY KEY(`email`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `medications` ADD COLUMN `stockCount` INTEGER NOT NULL DEFAULT 30")
                db.execSQL("ALTER TABLE `medications` ADD COLUMN `lowStockThreshold` INTEGER NOT NULL DEFAULT 5")
                db.execSQL("ALTER TABLE `intake_logs` ADD COLUMN `sideEffectNote` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medication_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

