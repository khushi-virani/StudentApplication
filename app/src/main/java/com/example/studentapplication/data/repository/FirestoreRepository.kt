package com.example.studentapplication.data.repository

import android.util.Log
import com.example.studentapplication.data.local.AttendanceEntity
import com.example.studentapplication.data.local.StudentEntity
import com.example.studentapplication.utils.CrashlyticsHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.type.Date
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val userId get() = auth.currentUser?.uid ?: ""

    // ── Students ──────────────────────────────────────────

    suspend fun syncStudentToCloud(student: StudentEntity) {    //upload students to firebase
        if (userId.isEmpty()) return
        try {
            db.collection("users")
                .document(userId)
                .collection("students")
                .document(student.id.toString())
                .set(student.toMap(), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            CrashlyticsHelper.recordError(e,"Failed to sync student : ${student.name}")
            e.printStackTrace()
        }
    }

    suspend fun deleteStudentFromCloud(studentId: Int) {
        if (userId.isEmpty()) return
        try {
            db.collection("users")
                .document(userId)
                .collection("students")
                .document(studentId.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchStudentsFromCloud(): List<StudentEntity> {
        if (userId.isEmpty()) return emptyList()
        return try {
            db.collection("users")
                .document(userId)
                .collection("students")
                .get()
                .await()
                .documents  //return each student document
                .mapNotNull { it.toStudentEntity() }    //convert firestore document into StudentEntity
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ── Attendance ────────────────────────────────────────

    suspend fun syncAttendanceToCloud(attendance: AttendanceEntity) {
        if (userId.isEmpty()) return
        try {
            val docId = "${attendance.studentId}_${attendance.date}"    //because each stud can have attendance for multiple days
            db.collection("users")
                .document(userId)
                .collection("attendance")
                .document(docId)
                .set(attendance.toMap(), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchAttendanceFromCloud(): List<AttendanceEntity> {
        if (userId.isEmpty()) return emptyList()
        return try {
            db.collection("users")
                .document(userId)
                .collection("attendance")
                .get()
                .await()
                .documents
                .mapNotNull { it.toAttendanceEntity() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getAttendanceByDateRange(
        startDate: String,
        endDate: String
    ): List<AttendanceEntity> {
        if (userId.isEmpty()) return emptyList()
        return try {
            db.collection("users")
                .document(userId)
                .collection("attendance")
                .whereGreaterThanOrEqualTo("date", startDate)   // ← range query
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()
                .documents
                .mapNotNull { it.toAttendanceEntity() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun listenToStudents(
        onUpdate: (List<StudentEntity>) -> Unit
    ): ListenerRegistration {    // ← returns registration so you can stop listening
        return db.collection("users")
            .document(userId)
            .collection("students")
            .addSnapshotListener { snapshot, error ->   // ← fires on every change
                if (error != null) {
                    Log.e("Firestore", "Listen error: ${error.message}")
                    return@addSnapshotListener  //stop this callback execution
                }
                val students = snapshot?.documents
                    ?.mapNotNull { it.toStudentEntity() }
                    ?: emptyList()
                onUpdate(students)  //pass latest students to caller
            }
    }

    fun listenToAttendance(
        onUpdate: (List<AttendanceEntity>) -> Unit
    ): ListenerRegistration {
        return db.collection("users")
            .document(userId)
            .collection("attendance")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                        Log.e("Firestore", "Listen error: ${error.message}")
                    return@addSnapshotListener
                }
                val attendance = snapshot?.documents
                    ?.mapNotNull { it.toAttendanceEntity() }
                    ?: emptyList()
                onUpdate(attendance)
            }
    }

    suspend fun sendNewStudentNotification(studentName: String) {
        if (userId.isEmpty()) return
        try {
            // Save notification record to Firestore
            // FCM topic message is sent via Firebase Console or Cloud Functions
            db.collection("notifications")
                .add(mapOf(
                    "title"     to "New Student Added",
                    "body"      to "$studentName has been added",
                    "userId"    to userId,
                    "timestamp" to System.currentTimeMillis()
                ))
                .await()
            Log.d("FCM", "Notification record saved")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    // ── Mappers ───────────────────────────────────────────

    private fun StudentEntity.toMap() = mapOf(  //convert kotlin obj to fireStore map
        "id"       to id,
        "userId"   to userId,
        "name"     to name,
        "course"   to course,
        "email"    to email,
        "phone"    to phone,
        "photoUri" to photoUri
    )

    private fun AttendanceEntity.toMap() = mapOf(
        "id"        to id,
        "studentId" to studentId,
        "userId"    to userId,
        "date"      to date,
        "status"    to status
    )

    private fun DocumentSnapshot.toStudentEntity(): StudentEntity? {    //firestore -> entity
        return try {
            StudentEntity(
                id       = (getLong("id") ?: 0).toInt(),    //reads from the firestore
                userId   = getString("userId") ?: "",
                name     = getString("name") ?: "",
                course   = getString("course") ?: "",
                email    = getString("email") ?: "",
                phone    = getString("phone") ?: "",
                photoUri = getString("photoUri") ?: ""
            )
        } catch (e: Exception) { null }
    }

    private fun DocumentSnapshot.toAttendanceEntity(): AttendanceEntity? {
        return try {
            AttendanceEntity(
                id        = (getLong("id") ?: 0).toInt(),
                studentId = (getLong("studentId") ?: 0).toInt(),
                userId    = getString("userId") ?: "",
                date      = getString("date") ?: "",
                status    = getString("status") ?: ""
            )
        } catch (e: Exception) { null }
    }
}