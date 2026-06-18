package com.example.studentapplication.data.model

data class Student(
    val id: Int,
    val name: String,
    val course: String,
    val email: String
)

//used to show data in UI
//not store in DB directly
