package com.example.studentapplication.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(student: StudentEntity)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    // All queries now filter by userId
    @Query("SELECT * FROM students WHERE userId = :userId ORDER BY id DESC")
    fun getAllStudents(userId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :id")
    fun getStudentById(id: Int): Flow<StudentEntity?>

    @Query("""
        SELECT * FROM students
        WHERE userId = :userId
        AND (name LIKE '%' || :query || '%'
        OR course LIKE '%' || :query || '%')
        ORDER BY id DESC
    """)
    fun searchStudents(query: String, userId: String): Flow<List<StudentEntity>>

    @Query("SELECT COUNT(*) FROM students WHERE userId = :userId")
    fun getStudentCount(userId: String): Flow<Int>

    @Query("SELECT * FROM students WHERE userId = :userId ORDER BY id DESC LIMIT 1")
    fun getLatestStudent(userId: String): Flow<StudentEntity?>

    @Query("SELECT * FROM students WHERE userId = :userId ORDER BY name ASC")
    fun getStudentsSortedByName(userId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE userId = :userId ORDER BY course ASC")
    fun getStudentsSortedByCourse(userId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE userId = :userId ORDER BY id ASC")
    fun getStudentsSortedByDateAsc(userId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE userId = :userId ORDER BY id DESC")
    fun getStudentsSortedByDateDesc(userId: String): Flow<List<StudentEntity>>
}