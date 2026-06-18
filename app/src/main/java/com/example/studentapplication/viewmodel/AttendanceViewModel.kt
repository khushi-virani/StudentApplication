package com.example.studentapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studentapplication.data.local.AttendanceEntity
import com.example.studentapplication.data.repository.AttendanceRepository
import com.example.studentapplication.data.repository.FirestoreRepository
import com.example.studentapplication.data.repository.StudentRepository
import com.example.studentapplication.utils.AnalyticsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val studentRepository: StudentRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _userId = MutableStateFlow("")
    private val _selectedDate = MutableStateFlow(todayDate())

    val selectedDate: StateFlow<String> = _selectedDate

    fun setUserId(id: String) { _userId.value = id }
    fun setDate(date: String) { _selectedDate.value = date }

    fun todayDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // All students for this user
    @OptIn(ExperimentalCoroutinesApi::class)
    val allStudents = _userId.flatMapLatest { userId ->
        if (userId.isEmpty()) flowOf(emptyList())
        else studentRepository.getAllStudents(userId)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Attendance records for selected date
    @OptIn(ExperimentalCoroutinesApi::class)
    val attendanceForDate = combine(_selectedDate, _userId) { date, userId ->
        Pair(date, userId)
    }.flatMapLatest { (date, userId) ->
        if (userId.isEmpty()) flowOf(emptyList())
        else attendanceRepository.getAttendanceForDate(date, userId)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Mark attendance — insert replaces existing record for same student+date
    fun markAttendance(studentId: Int, status: String) {
        viewModelScope.launch {
            attendanceRepository.insertAttendance(
                AttendanceEntity(
                    studentId = studentId,
                    userId    = _userId.value,
                    date      = _selectedDate.value,
                    status    = status
                )
            )
            AnalyticsHelper.logAttendanceMarked(status,_selectedDate.value)
        }
    }

    // Get attendance history for one student
    fun getAttendanceForStudent(studentId: Int): Flow<List<AttendanceEntity>> =
        attendanceRepository.getAttendanceForStudent(studentId, _userId.value)

    // Get present count for a student
    fun getPresentCount(studentId: Int): Flow<Int> =
        attendanceRepository.getPresentCount(studentId, _userId.value)

    // Get total count for a student
    fun getTotalCount(studentId: Int): Flow<Int> =
        attendanceRepository.getTotalCount(studentId, _userId.value)

    suspend fun getAttendanceByDateRange(   //simply forward the request
        startDate: String,
        endDate: String
    ): List<AttendanceEntity> {
        return firestoreRepository.getAttendanceByDateRange(startDate, endDate)
    }
}