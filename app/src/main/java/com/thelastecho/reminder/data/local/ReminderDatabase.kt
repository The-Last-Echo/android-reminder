package com.thelastecho.reminder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.thelastecho.reminder.data.local.dao.CategoryDao
import com.thelastecho.reminder.data.local.dao.ReminderDao
import com.thelastecho.reminder.data.local.entity.CategoryEntity
import com.thelastecho.reminder.data.local.entity.ReminderEntity
import com.thelastecho.reminder.data.local.entity.SubTaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ReminderEntity::class,
        SubTaskEntity::class,
        CategoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ReminderDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        private const val DATABASE_NAME = "reminder_database.db"

        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        fun getInstance(context: Context): ReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): ReminderDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ReminderDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default categories
                        CoroutineScope(Dispatchers.IO).launch {
                            val categoryDao = getInstance(context).categoryDao()
                            categoryDao.insertCategory(
                                CategoryEntity(name = "Personal", colorArgb = 0xFF4CAF50, iconName = "person")
                            )
                            categoryDao.insertCategory(
                                CategoryEntity(name = "Work", colorArgb = 0xFF2196F3, iconName = "work")
                            )
                            categoryDao.insertCategory(
                                CategoryEntity(name = "Shopping", colorArgb = 0xFFFF9800, iconName = "shopping_cart")
                            )
                        }
                    }
                })
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE reminders ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE reminders ADD COLUMN deletedAt INTEGER")
            }
        }
    }
}
