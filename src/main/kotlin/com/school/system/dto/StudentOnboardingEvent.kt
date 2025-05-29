package com.school.system.dto

import java.time.LocalDate

data class StudentOnboardingEvent(
    val aadhaar: String,
    val rollNo: String,
    val name: String,
    val studentClass: String,
    val school: String = "ABC Public School",
    val dob: LocalDate

)
