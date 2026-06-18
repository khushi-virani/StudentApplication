package com.example.studentapplication.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    // Insert or replace — if same studentId+date exists, overwrite it
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(attendance: AttendanceEntity)

    // All attendance records for a specific student
    @Query("SELECT * FROM attendance WHERE studentId = :studentId AND userId = :userId ORDER BY date DESC")
    fun getAttendanceForStudent(studentId: Int, userId: String): Flow<List<AttendanceEntity>>

    // Attendance for ALL students on a specific date
    @Query("SELECT * FROM attendance WHERE date = :date AND userId = :userId")
    fun getAttendanceForDate(date: String, userId: String): Flow<List<AttendanceEntity>>

    // Count of Present records for a student — used to calculate %
    @Query("SELECT COUNT(*) FROM attendance WHERE studentId = :studentId AND userId = :userId AND status = 'Present'")
    fun getPresentCount(studentId: Int, userId: String): Flow<Int>

    // Total records for a student — used to calculate %
    @Query("SELECT COUNT(*) FROM attendance WHERE studentId = :studentId AND userId = :userId")
    fun getTotalCount(studentId: Int, userId: String): Flow<Int>

    // Check if attendance already marked for a student on a date
    @Query("SELECT * FROM attendance WHERE studentId = :studentId AND date = :date AND userId = :userId LIMIT 1")
    fun getAttendanceForStudentOnDate(studentId: Int, date: String, userId: String): Flow<AttendanceEntity?>
}