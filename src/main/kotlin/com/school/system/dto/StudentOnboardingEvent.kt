package com.school.system.dto

import java.time.LocalDate

/**
 * Data Transfer Object representing a student onboarding event.
 *
 * This DTO is used for serializing and deserializing student onboarding data
 * when communicating between services via Kafka or REST.
 *
 * @property aadhaar Unique 12-digit identity number of the student (must be anonymized in production).
 * @property rollNo Unique roll number assigned to the student by the school.
 * @property name Full name of the student.
 * @property studentClass The class/grade the student is enrolled in.
 * @property school Name of the school. Default is "ABC Public School" to simplify test/demo data.
 * @property dob Date of birth of the student, formatted as ISO LocalDate.
 */
data class StudentOnboardingEvent(
    val aadhaar: String,
    val rollNo: String,
    val name: String,
    val studentClass: String,
    val school: String = "ABC Public School",
    val dob: LocalDate

)
