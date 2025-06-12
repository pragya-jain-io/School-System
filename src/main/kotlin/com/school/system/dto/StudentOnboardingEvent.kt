package com.school.system.dto

import java.time.LocalDate

/**
 * Data Transfer Object representing a student onboarding event.
 *
 * This DTO is used for serializing and deserializing student onboarding data
 * when communicating between services via Kafka or REST.
 *
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
