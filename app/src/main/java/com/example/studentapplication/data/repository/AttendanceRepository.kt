package com.example.studentapplication.data.repository

import android.util.Log
import com.example.studentapplication.data.local.AttendanceDao
import com.example.studentapplication.data.local.AttendanceEntity
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepository @Inject constructor(
    private val attendanceDao: AttendanceDao,
    private val firestoreRepository: FirestoreRepository,
    private val appScope: CoroutineScope
) {
    suspend fun insertAttendance(attendance: AttendanceEntity) {
        attendanceDao.insertAttendance(attendance)
        appScope.launch {
            firestoreRepository.syncAttendanceToCloud(attendance)
        }
    }

    suspend fun syncFromCloud() {
        val cloudAttendance = firestoreRepository.fetchAttendanceFromCloud()
        cloudAttendance.forEach { attendanceDao.insertOrIgnore(it) }
    }

    fun startRealtimeSync(scope: CoroutineScope): ListenerRegistration {
        return firestoreRepository.listenToAttendance { cloudAttendance ->  //when firestore changes it sends data here
            scope.launch {
                cloudAttendance.forEach { attendanceDao.insertOrIgnore(it) }
                Log.d("Firestore", "Real-time sync: ${cloudAttendance.size} attendance updated")
            }
        }
    }

    fun getAttendanceForStudent(studentId: Int, userId: String): Flow<List<AttendanceEntity>> =
        attendanceDao.getAttendanceForStudent(studentId, userId)

    fun getAttendanceForDate(date: String, userId: String): Flow<List<AttendanceEntity>> =
        attendanceDao.getAttendanceForDate(date, userId)

    fun getPresentCount(studentId: Int, userId: String): Flow<Int> =
        attendanceDao.getPresentCount(studentId, userId)

    fun getTotalCount(studentId: Int, userId: String): Flow<Int> =
        attendanceDao.getTotalCount(studentId, userId)

    fun getAttendanceForStudentOnDate(studentId: Int, date: String, userId: String): Flow<AttendanceEntity?> =
        attendanceDao.getAttendanceForStudentOnDate(studentId, date, userId)
}