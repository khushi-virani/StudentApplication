package com.example.studentapplication

import com.example.studentapplication.data.local.AttendanceDao
import com.example.studentapplication.data.local.AttendanceEntity
import com.example.studentapplication.data.repository.AttendanceRepository
import com.example.studentapplication.data.repository.FirestoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceRepositoryTest {

    private lateinit var attendanceDao: AttendanceDao
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var attendanceRepository: AttendanceRepository
    private lateinit var appScope: CoroutineScope

    private val testAttendance = AttendanceEntity(
        id        = 1,
        studentId = 1,
        userId    = "user123",
        date      = "2024-01-15",
        status    = "Present"
    )

    @Before
    fun setup() {
        attendanceDao       = mock()
        firestoreRepository = mock()
        appScope            = CoroutineScope(Dispatchers.Unconfined)

        attendanceRepository = AttendanceRepository(
            attendanceDao       = attendanceDao,
            firestoreRepository = firestoreRepository,
            appScope            = appScope
        )
    }

    // ── Insert Tests ──────────────────────────────────────

    @Test
    fun `insertAttendance calls dao insertAttendance`() = runTest {
        // When
        attendanceRepository.insertAttendance(testAttendance)

        // Then
        verify(attendanceDao).insertAttendance(testAttendance)
    }

    @Test
    fun `insertAttendance syncs to Firestore`() = runTest {
        // When
        attendanceRepository.insertAttendance(testAttendance)

        // Then
        verify(firestoreRepository).syncAttendanceToCloud(testAttendance)
    }

    // ── Query Tests ───────────────────────────────────────

    @Test
    fun `getAttendanceForDate returns correct records`() = runTest {
        // Given
        val attendanceList = listOf(testAttendance)
        whenever(attendanceDao.getAttendanceForDate("2024-01-15", "user123"))
            .thenReturn(flowOf(attendanceList))

        // When
        val result = attendanceRepository.getAttendanceForDate("2024-01-15", "user123")

        // Then
        result.collect { list ->
            assertEquals(1, list.size)
            assertEquals("Present", list[0].status)
        }
    }

    @Test
    fun `getPresentCount returns correct count`() = runTest {
        // Given
        whenever(attendanceDao.getPresentCount(1, "user123")).thenReturn(flowOf(8))

        // When
        val result = attendanceRepository.getPresentCount(1, "user123")

        // Then
        result.collect { count ->
            assertEquals(8, count)
        }
    }

    @Test
    fun `getTotalCount returns correct count`() = runTest {
        // Given
        whenever(attendanceDao.getTotalCount(1, "user123")).thenReturn(flowOf(10))

        // When
        val result = attendanceRepository.getTotalCount(1, "user123")

        // Then
        result.collect { count ->
            assertEquals(10, count)
        }
    }

    @Test
    fun `getAttendanceForStudentOnDate returns correct record`() = runTest {
        // Given
        whenever(attendanceDao.getAttendanceForStudentOnDate(1, "2024-01-15", "user123"))
            .thenReturn(flowOf(testAttendance))

        // When
        val result = attendanceRepository.getAttendanceForStudentOnDate(1, "2024-01-15", "user123")

        // Then
        result.collect { attendance ->
            assertNotNull(attendance)
            assertEquals("Present", attendance?.status)
        }
    }
}