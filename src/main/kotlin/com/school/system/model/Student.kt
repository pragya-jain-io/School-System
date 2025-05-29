package com.school.system.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "students")
data class Student(
    @Id
    val aadhaar: String,
    val rollNo: String,
    val name: String,
    val studentClass: String,
    val school: String = "ABC Public School"
)