package com.example.studentapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val userId: String = "",

    val name: String,

    val course: String,

    val email: String,

    val phone: String,

    val photoUri: String
)

