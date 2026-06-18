package com.example.studentapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [StudentEntity::class, AttendanceEntity::class],
    version = 5,
    exportSchema = false
)
abstract class StudentDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {

        @Volatile   //ensure all thread see the same value , preventing caching issue
        private var INSTANCE: StudentDatabase? = null

        // Migration from version 1 to 2 — adds userId column
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE students ADD COLUMN photoUri TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        // v3 → v4: creates attendance table
        val Migration_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS attendance (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        studentId INTEGER NOT NULL,
                        userId TEXT NOT NULL,
                        date TEXT NOT NULL,
                        status TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
        // Version 4 → 5: adds unique constraint on attendance table
        val Migration_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL("""
            CREATE TABLE attendance_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                studentId INTEGER NOT NULL,
                userId TEXT NOT NULL,
                date TEXT NOT NULL,
                status TEXT NOT NULL
            )
        """.trimIndent())

                database.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS
            index_attendance_studentId_date_userId
            ON attendance_new(studentId, date, userId)
        """.trimIndent())

                database.execSQL("""
            INSERT OR IGNORE INTO attendance_new
            (id, studentId, userId, date, status)
            SELECT id, studentId, userId, date, status
            FROM attendance
        """.trimIndent())

                database.execSQL("DROP TABLE attendance")
                database.execSQL("ALTER TABLE attendance_new RENAME TO attendance")
            }
        }

        fun getDatabase(context: Context): StudentDatabase {    //create and return database instance

            return INSTANCE ?: synchronized(this) {//if instance is not null then return it
                                                        //else only 1 thread can enter this block at time , prevent multiple db being created
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudentDatabase::class.java,
                    "student_database"
                )
                    .addMigrations(MIGRATION_2_3,Migration_3_4,Migration_4_5)
                    .build()

                INSTANCE = instance //store db so next time reuse it

                instance    //give db to the app
            }
        }
    }
}