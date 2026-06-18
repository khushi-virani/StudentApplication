package com.example.studentapplication

import com.example.studentapplication.data.local.StudentDao
import com.example.studentapplication.data.local.StudentEntity
import com.example.studentapplication.data.repository.FirestoreRepository
import com.example.studentapplication.data.repository.StudentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class StudentRepositoryTest {

    // Mocks — fake versions of dependencies
    private lateinit var studentDao: StudentDao
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var studentRepository: StudentRepository
    private lateinit var appScope: CoroutineScope

    private val testStudent = StudentEntity(
        id       = 1,
        userId   = "user123",
        name     = "John Doe",
        course   = "BCA",
        email    = "john@gmail.com",
        phone    = "1234567890",
        photoUri = ""
    )

    @Before //runs before every test
    fun setup() {
        // Create mock objects
        studentDao          = mock()
        firestoreRepository = mock()
        appScope            = CoroutineScope(Dispatchers.Unconfined)    //create simple coroutine scope for testing

        studentRepository = StudentRepository(
            studentDao          = studentDao,
            firestoreRepository = firestoreRepository,
            appScope            = appScope
        )
    }

    // ── Insert Tests ──────────────────────────────────────

    @Test
    fun `insertStudent calls dao insertStudent`() = runTest {
        // Given
        whenever(studentDao.insertStudent(testStudent)).thenReturn(1L)

        // When
        studentRepository.insertStudent(testStudent)

        // Then — verify dao was called
        verify(studentDao).insertStudent(testStudent)
    }

    @Test   //test-2 check cloud sync
    fun `insertStudent syncs to Firestore`() = runTest {
        // Given
        whenever(studentDao.insertStudent(testStudent)).thenReturn(1L)

        // When
        studentRepository.insertStudent(testStudent)

        // Then — verify Firestore sync was called
        verify(firestoreRepository).syncStudentToCloud(testStudent.copy(id = 1))
    }

    // ── Update Tests ──────────────────────────────────────

    @Test
    fun `updateStudent calls dao updateStudent`() = runTest {
        // When
        studentRepository.updateStudent(testStudent)

        // Then
        verify(studentDao).updateStudent(testStudent)
    }

    @Test
    fun `updateStudent syncs to Firestore`() = runTest {
        // When
        studentRepository.updateStudent(testStudent)

        // Then
        verify(firestoreRepository).syncStudentToCloud(testStudent)
    }

    // ── Delete Tests ──────────────────────────────────────

    @Test
    fun `deleteStudent calls dao deleteStudent`() = runTest {
        // When
        studentRepository.deleteStudent(testStudent)

        // Then
        verify(studentDao).deleteStudent(testStudent)
    }

    @Test
    fun `deleteStudent removes from Firestore`() = runTest {
        // When
        studentRepository.deleteStudent(testStudent)

        // Then
        verify(firestoreRepository).deleteStudentFromCloud(testStudent.id)
    }

    // ── Query Tests ───────────────────────────────────────

    @Test
    fun `getAllStudents returns flow from dao`() = runTest {
        // Given
        val studentList = listOf(testStudent)
        whenever(studentDao.getAllStudents("user123")).thenReturn(flowOf(studentList))

        // When
        val result = studentRepository.getAllStudents("user123")

        // Then
        result.collect { list ->
            assertEquals(1, list.size)
            assertEquals("John Doe", list[0].name)
        }
    }

    @Test
    fun `getStudentCount returns correct count`() = runTest {
        // Given
        whenever(studentDao.getStudentCount("user123")).thenReturn(flowOf(5))

        // When
        val result = studentRepository.getStudentCount("user123")

        // Then
        result.collect { count ->
            assertEquals(5, count)
        }
    }

    @Test
    fun `searchStudents returns filtered list`() = runTest {
        // Given
        val searchResult = listOf(testStudent)
        whenever(studentDao.searchStudents("John", "user123"))
            .thenReturn(flowOf(searchResult))

        // When
        val result = studentRepository.searchStudents("John", "user123")

        // Then
        result.collect { list ->
            assertEquals(1, list.size)
            assertEquals("John Doe", list[0].name)
        }
    }
}
