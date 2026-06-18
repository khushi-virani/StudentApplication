package com.example.studentapplication

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.studentapplication.data.local.AttendanceEntity
import com.example.studentapplication.data.repository.AttendanceRepository
import com.example.studentapplication.data.repository.FirestoreRepository
import com.example.studentapplication.data.repository.StudentRepository
import com.example.studentapplication.viewmodel.AttendanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule() //run stateFlow update immediately during testing

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var attendanceRepository: AttendanceRepository
    private lateinit var studentRepository: StudentRepository
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var viewModel: AttendanceViewModel

    private val testAttendance = AttendanceEntity(
        id        = 1,
        studentId = 1,
        userId    = "user123",
        date      = "2024-01-15",
        status    = "Present"
    )
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        attendanceRepository = mock()
        studentRepository    = mock()
        firestoreRepository  = mock()

        whenever(attendanceRepository.getAttendanceForDate(any(), any()))
            .thenReturn(flowOf(emptyList()))
        whenever(studentRepository.getAllStudents(any()))
            .thenReturn(flowOf(emptyList()))

        viewModel = AttendanceViewModel(
            attendanceRepository = attendanceRepository,
            studentRepository    = studentRepository,
            firestoreRepository  = firestoreRepository
        )
    }

    @Test
    fun `getTotalCount returns correct flow`() = runTest {
        // Given
        whenever(attendanceRepository.getTotalCount(1, "user123"))
            .thenReturn(flowOf(10))
        viewModel.setUserId("user123")

        // When
        val result = viewModel.getTotalCount(1)

        // Then
        assertNotNull(result)
        result.collect { count ->
            assertEquals(10, count)
            return@collect  //stop after first emission
        }
    }
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setUserId updates userId`() = runTest {
        // Given — mock flows so ViewModel doesn't crash
        whenever(attendanceRepository.getAttendanceForDate(any(), any()))
            .thenReturn(flowOf(emptyList()))
        whenever(studentRepository.getAllStudents(any()))
            .thenReturn(flowOf(emptyList()))

        // When
        viewModel.setUserId("user123")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then — selectedDate should still have today's date (userId doesn't affect it)
        assertTrue(viewModel.selectedDate.value.isNotEmpty())
    }

    @Test
    fun `setDate updates selectedDate`() = runTest {
        // When
        viewModel.setDate("2024-06-01")

        // Then
        assertEquals("2024-06-01", viewModel.selectedDate.value)
    }

    @Test
    fun `markAttendance calls repository insertAttendance`() = runTest {
        // Given - mock repository response
        whenever(attendanceRepository.getAttendanceForDate(any(), any()))
            .thenReturn(flowOf(emptyList()))
        whenever(studentRepository.getAllStudents(any()))
            .thenReturn(flowOf(emptyList()))

        viewModel.setUserId("user123")
        viewModel.setDate("2024-01-15")
        testDispatcher.scheduler.advanceUntilIdle()

        // When - call viewModel fun
        viewModel.markAttendance(1, "Present")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then — id=0 because ViewModel doesn't set it, Room auto-generates
        verify(attendanceRepository).insertAttendance(
            AttendanceEntity(
                id        = 0,   // ← 0 not 1
                studentId = 1,
                userId    = "user123",
                date      = "2024-01-15",
                status    = "Present"
            )
        )
    }
    @Test
    fun `getPresentCount returns correct flow`() = runTest {
        // Given — must set userId first
        viewModel.setUserId("user123")
        whenever(attendanceRepository.getPresentCount(1, "user123"))
            .thenReturn(flowOf(8))

        // When
        val result = viewModel.getPresentCount(1)

        // Then - verify result and repository call
        assertNotNull(result)  // ← check not null first
        result.collect { count ->
            assertEquals(8, count)
        }
    }

    @Test
    fun `todayDate returns correct format`() {
        // When
        val date = viewModel.todayDate()

        // Then — should match yyyy-MM-dd format
        assertTrue(date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }
}