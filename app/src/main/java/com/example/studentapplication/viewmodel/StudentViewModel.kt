package com.example.studentapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Entity
import com.example.studentapplication.data.local.StudentDao
import com.example.studentapplication.data.local.StudentEntity
import com.example.studentapplication.data.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentViewModel @Inject constructor(
    private val repository: StudentRepository
) : ViewModel() {

    private val _userId = MutableStateFlow("")

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    fun setUserId(id: String) {
        _userId.value = id
    }
    fun isUserIdSet(): Boolean = _userId.value.isNotEmpty()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _sortOption = MutableStateFlow(SortOption.DEFAULT)

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    // Students list — reacts to userId + search + sort changes
    @OptIn(ExperimentalCoroutinesApi::class)
    val students: StateFlow<List<StudentEntity>> = combine(
        _userId, _searchQuery, _sortOption
    ) { userId, query, sort ->
        Triple(userId, query, sort)
    }.flatMapLatest { (userId, query, sort) ->
        if (userId.isEmpty()) return@flatMapLatest flowOf(emptyList())
        when {
            query.isNotEmpty() -> repository.searchStudents(query, userId)
            sort == SortOption.NAME -> repository.getStudentsSortedByName(userId)
            sort == SortOption.COURSE -> repository.getStudentsSortedByCourse(userId)
            sort == SortOption.DATE_ASC -> repository.getStudentsSortedByDateAsc(userId)
            sort == SortOption.DATE_DESC -> repository.getStudentsSortedByDateDesc(userId)
            else -> repository.getAllStudents(userId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    //  Student count as StateFlow
    @OptIn(ExperimentalCoroutinesApi::class)
    val studentCount: StateFlow<Int> = _userId
        .flatMapLatest { userId ->
            if (userId.isEmpty()) flowOf(0)
            else repository.getStudentCount(userId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0
        )

    //  Latest student as StateFlow
    @OptIn(ExperimentalCoroutinesApi::class)
    val latestStudent: StateFlow<StudentEntity?> = _userId
        .flatMapLatest { userId ->
            if (userId.isEmpty()) flowOf(null)
            else repository.getLatestStudent(userId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    //  Single student detail as StateFlow
    private val _studentId = MutableStateFlow(-1)

    fun setStudentId(id: Int) {
        _studentId.value = id
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val studentDetail: StateFlow<StudentEntity?> = _studentId
        .flatMapLatest { id ->
            if (id == -1) flowOf(null)
            else repository.getStudentById(id)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    //  CRUD operations
    fun insertStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.insertStudent(student.copy(userId = _userId.value))
        }
    }

    fun updateStudent(student: StudentEntity) {
        viewModelScope.launch { repository.updateStudent(student) }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch { repository.deleteStudent(student) }
    }
}

// Sort options enum
enum class SortOption {
    DEFAULT,
    NAME,
    COURSE,
    DATE_ASC,
    DATE_DESC
}
