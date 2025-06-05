package com.school.system.service

import com.school.system.dto.StudentOnboardingEvent
import com.school.system.model.Student
import com.school.system.repository.StudentRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Service class responsible for managing Student domain logic.
 * Provides functionality to create new students and fetch existing students by Aadhaar.
 * Integrates with Kafka to publish onboarding events upon student creation.
 */
@Service
class StudentService(
    private val studentRepository: StudentRepository,
    private val kafkaTemplate: KafkaTemplate<String, StudentOnboardingEvent>
) {

    private val logger = LoggerFactory.getLogger(StudentService::class.java)
    private val topic = "student-onboarding"

    /**
     * Saves a new Student entity to the database and publishes
     * a StudentOnboardingEvent to the Kafka topic "student-onboarding".
     *
     * @param student domain Student object to be created
     * @return Mono<Student> that emits the saved student on success
     */
    fun createStudent(student: Student): Mono<Student> {
        return studentRepository.save(student)
            .doOnSuccess {
                val event = StudentOnboardingEvent(
                    aadhaar = it.aadhaar,
                    rollNo = it.rollNo,
                    name = it.name,
                    studentClass = it.studentClass,
                    dob = it.dob,
                    school = it.school
                )
                logger.info("Student saved and onboarding event published for rollNo=${it.rollNo}, aadhaar=${it.aadhaar}")
                // Publishing the event asynchronously to "student-onboarding" Kafka topic
                kafkaTemplate.send(topic, event)
            }
            .doOnError {
                logger.error("Failed to save student with aadhaar=${student.aadhaar}", it)
            }
    }

}
