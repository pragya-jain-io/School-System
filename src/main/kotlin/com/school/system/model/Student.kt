package com.school.system.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

/**
 * Represents a student entity stored in the 'students' collection.
 *
 * Contains essential details for student identification and enrollment.
 */
@Document(collection = "students")
data class Student(
    @Id
    val aadhaar: String,
    val rollNo: String,
    val name: String,
    val studentClass: String,
    val school: String = "ABC Public School",
    val dob: LocalDate
)