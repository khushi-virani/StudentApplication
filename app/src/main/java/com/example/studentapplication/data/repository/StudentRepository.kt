package com.example.studentapplication.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.studentapplication.data.local.StudentDao
import com.example.studentapplication.data.local.StudentEntity
import com.example.studentapplication.utils.AnalyticsHelper
import com.example.studentapplication.utils.CrashlyticsHelper
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton  //only one repository obj exists
class StudentRepository @Inject constructor(
    private val studentDao: StudentDao,
    private val firestoreRepository: FirestoreRepository,
    private val appScope: CoroutineScope
)  {

    suspend fun insertStudent(student: StudentEntity) {
        val id = studentDao.insertStudent(student).toInt()
        val saved = student.copy(id = id)
        CrashlyticsHelper.log("Student Added : ${student.name}")
        AnalyticsHelper.logStudentAdded(student.name, student.course)
        appScope.launch {
            firestoreRepository.syncStudentToCloud(saved)
            // Notify all users about new student
            firestoreRepository.sendNewStudentNotification(saved.name)
        }
    }

    suspend fun updateStudent(student: StudentEntity) {
        studentDao.updateStudent(student)
        CrashlyticsHelper.log("Student Updated : ${student.name}")
        AnalyticsHelper.logStudentUpdated(student.name)
        appScope.launch {
            firestoreRepository.syncStudentToCloud(student)
        }
    }

    suspend fun deleteStudent(student: StudentEntity) {
        studentDao.deleteStudent(student)
        CrashlyticsHelper.log("Student Deleted : ${student.name}")
        AnalyticsHelper.logStudentDeleted(student.name)
        appScope.launch {
            firestoreRepository.deleteStudentFromCloud(student.id)
        }
    }

    suspend fun syncFromCloud(userId: String) { //this is reverse process now firebase -> room
        val cloudStudents = firestoreRepository.fetchStudentsFromCloud()
        cloudStudents.forEach { studentDao.insertOrIgnore(it) }
    }

    fun startRealtimeSync(scope: CoroutineScope): ListenerRegistration {    //listen to firestore & update room automatically
        return firestoreRepository.listenToStudents { cloudStudents ->
            scope.launch {
                cloudStudents.forEach { studentDao.insertOrReplace(it) }  // ← REPLACE not IGNORE
                Log.d("Firestore", "Real-time sync: ${cloudStudents.size} students updated")
            }
        }
    }
    fun getStudentById(id: Int): Flow<StudentEntity?> =
        studentDao.getStudentById(id)

    fun getAllStudents(userId: String) : Flow<List<StudentEntity>> =
        studentDao.getAllStudents(userId)

    fun searchStudents(query: String, userId: String): Flow<List<StudentEntity>> =
        studentDao.searchStudents(query, userId)

    fun getStudentCount(userId: String): Flow<Int> =
        studentDao.getStudentCount(userId)

    fun getLatestStudent(userId: String): Flow<StudentEntity?> =
        studentDao.getLatestStudent(userId)

    fun getStudentsSortedByName(userId: String): Flow<List<StudentEntity>> =
        studentDao.getStudentsSortedByName(userId)

    fun getStudentsSortedByCourse(userId: String): Flow<List<StudentEntity>> =
        studentDao.getStudentsSortedByCourse(userId)

    fun getStudentsSortedByDateAsc(userId: String): Flow<List<StudentEntity>> =
        studentDao.getStudentsSortedByDateAsc(userId)

    fun getStudentsSortedByDateDesc(userId: String): Flow<List<StudentEntity>> =
        studentDao.getStudentsSortedByDateDesc(userId)
}