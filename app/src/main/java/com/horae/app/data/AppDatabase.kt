package com.horae.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ScheduleEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        /** v2：新增自定义重复与结束重复字段 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE schedules ADD COLUMN repeatFreq INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE schedules ADD COLUMN repeatInterval INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE schedules ADD COLUMN repeatEndType INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE schedules ADD COLUMN repeatEndDate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE schedules ADD COLUMN repeatCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "horae.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
