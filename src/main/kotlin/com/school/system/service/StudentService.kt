package com.school.system.service

import com.school.system.dto.StudentOnboardingEvent
import com.school.system.model.Student
import com.school.system.repository.StudentRepository
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class StudentService(
    private val studentRepository: StudentRepository,
    private val kafkaTemplate: KafkaTemplate<String, StudentOnboardingEvent>
) {

    fun createStudent(student: Student): Mono<Student> {
        return studentRepository.save(student)
            .doOnSuccess {
                val event = StudentOnboardingEvent(
                    aadhaar = it.aadhaar,
                    rollNo = it.rollNo,
                    name = it.name,
                    studentClass = it.studentClass,
                    school = it.school
                )
                kafkaTemplate.send("student-onboarding", event)
            }
    }

    fun getStudentByAadhaar(aadhaar: String): Mono<Student> = studentRepository.findById(aadhaar)
}
