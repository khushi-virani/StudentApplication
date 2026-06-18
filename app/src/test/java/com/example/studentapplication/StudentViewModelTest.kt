package com.example.studentapplication

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.studentapplication.data.local.StudentEntity
import com.example.studentapplication.data.repository.StudentRepository
import com.example.studentapplication.viewmodel.SortOption
import com.example.studentapplication.viewmodel.StudentViewModel
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class StudentViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule() //execute LiveData Task immediately

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var studentRepository: StudentRepository
    private lateinit var viewModel: StudentViewModel
    private val testStudent = StudentEntity(
        id       = 1,
        userId   = "user123",
        name     = "John Doe",
        course   = "BCA",
        email    = "john@gmail.com",
        phone    = "1234567890",
        photoUri = ""
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        studentRepository = mock()

        // Mock all flows so StateFlow initializes without crash
        whenever(studentRepository.getAllStudents("")).thenReturn(flowOf(emptyList()))
        whenever(studentRepository.getStudentCount("")).thenReturn(flowOf(0))
        whenever(studentRepository.getLatestStudent("")).thenReturn(flowOf(null))

        viewModel = StudentViewModel(studentRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── userId Tests ──────────────────────────────────────

    @Test
    fun `setUserId triggers student count update`() = runTest {
        // Given
        whenever(studentRepository.getStudentCount("user123")).thenReturn(flowOf(5))
        whenever(studentRepository.getAllStudents("user123")).thenReturn(flowOf(emptyList()))
        whenever(studentRepository.getLatestStudent("user123")).thenReturn(flowOf(null))

        // When
        viewModel.setUserId("user123")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(5, viewModel.studentCount.value)
    }

    @Test
    fun `isUserIdSet returns false when userId is empty`() {
        // userId is "" by default
        assertFalse(viewModel.isUserIdSet())
    }

    @Test
    fun `isUserIdSet returns true after setUserId`() = runTest {
        // Given
        whenever(studentRepository.getAllStudents("user123")).thenReturn(flowOf(emptyList()))
        whenever(studentRepository.getStudentCount("user123")).thenReturn(flowOf(0))
        whenever(studentRepository.getLatestStudent("user123")).thenReturn(flowOf(null))

        // When
        viewModel.setUserId("user123")

        // Then
        assertTrue(viewModel.isUserIdSet())
    }

    // ── Search Tests ──────────────────────────────────────

    @Test
    fun `setSearchQuery updates searchQuery state`() {
        // When
        viewModel.setSearchQuery("John")

        // Then
        assertEquals("John", viewModel.searchQuery.value)
    }

    @Test
    fun `setSearchQuery triggers search in repository`() = runTest {
        // Given
        whenever(studentRepository.getAllStudents("user123")).thenReturn(flowOf(emptyList()))
        whenever(studentRepository.getStudentCount("user123")).thenReturn(flowOf(0))
        whenever(studentRepository.getLatestStudent("user123")).thenReturn(flowOf(null))
        whenever(studentRepository.searchStudents("John", "user123"))
            .thenReturn(flowOf(listOf(testStudent)))

        // When
        viewModel.setUserId("user123")
        viewModel.setSearchQuery("John")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.students.value.size)
        assertEquals("John Doe", viewModel.students.value[0].name)
    }

    // ── Sort Tests ────────────────────────────────────────

    @Test
    fun `setSortOption NAME triggers sorted query`() = runTest {
        // Given
        val sortedList = listOf(testStudent)
        whenever(studentRepository.getAllStudents("user123")).thenReturn(flowOf(emptyList()))
        whenever(studentRepository.getStudentCount("user123")).thenReturn(flowOf(0))
        whenever(studentRepository.getLatestStudent("user123")).thenReturn(flowOf(null))
        whenever(studentRepository.getStudentsSortedByName("user123"))
            .thenReturn(flowOf(sortedList))

        // When
        viewModel.setUserId("user123")
        viewModel.setSortOption(SortOption.NAME)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.students.value.size)
    }

    // ── CRUD Tests ────────────────────────────────────────

    @Test
    fun `deleteStudent calls repository deleteStudent`() = runTest {
        // When
        viewModel.deleteStudent(testStudent)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(studentRepository).deleteStudent(testStudent)
    }

    @Test
    fun `updateStudent calls repository updateStudent`() = runTest {
        // When
        viewModel.updateStudent(testStudent)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(studentRepository).updateStudent(testStudent)
    }

    @Test
    fun `insertStudent appends current userId to student`() = runTest {
        // Given
        whenever(studentRepository.getAllStudents("user123")).thenReturn(flowOf(emptyList()))
        whenever(studentRepository.getStudentCount("user123")).thenReturn(flowOf(0))
        whenever(studentRepository.getLatestStudent("user123")).thenReturn(flowOf(null))
        viewModel.setUserId("user123")

        // When
        viewModel.insertStudent(testStudent.copy(userId = ""))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then — repository receives student with correct userId
        verify(studentRepository).insertStudent(testStudent.copy(userId = "user123"))
    }

    // ── studentDetail Tests ───────────────────────────────

    @Test
    fun `setStudentId loads student detail`() = runTest {
        // Given
        whenever(studentRepository.getStudentById(1)).thenReturn(flowOf(testStudent))

        // When
        viewModel.setStudentId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("John Doe", viewModel.studentDetail.value?.name)
    }

    @Test
    fun `initial studentDetail is null`() {
        // studentId starts at -1
        assertNull(viewModel.studentDetail.value)
    }
}