package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.LocalPersonalShayariEntity
import com.example.data.model.ShayariEntity

@Database(
    entities = [ShayariEntity::class, LocalPersonalShayariEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ShayariDatabase : RoomDatabase() {
    abstract fun shayariDao(): ShayariDao
    abstract fun localPersonalShayariDao(): LocalPersonalShayariDao

    companion object {
        @Volatile
        private var INSTANCE: ShayariDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_personal_shayari` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `themeTag` TEXT NOT NULL,
                        `couplet` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): ShayariDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): ShayariDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ShayariDatabase::class.java,
                "shayari_anthology.db"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
