package com.school.system.consumer

import com.school.system.dto.StudentOnboardingEvent
import com.school.system.model.RetryEvent
import com.school.system.model.enum.RetryStatus
import com.school.system.repository.RetryEventRepository
import com.school.system.service.RetryEventService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

@Component
class StudentOnboardingConsumer(
    private val retryEventRepository: RetryEventRepository
) {
    private val logger = LoggerFactory.getLogger(StudentOnboardingConsumer::class.java)

    @KafkaListener(
        topics = ["student-onboarding"],
        groupId = "school-service",
        containerFactory = "kafkaListenerFactory"
    )
    fun listen(event: StudentOnboardingEvent) {
        logger.info("Consumed Kafka event: $event")

        val aadhaarLastDigit = event.aadhaar.last()

        val (httpStatus, responseMessage) = when (aadhaarLastDigit) {
            '0' -> HttpStatus.OK to "Student enrolled successfully"
            '1' -> HttpStatus.CONFLICT to "Student already enrolled"
            '2' -> HttpStatus.INTERNAL_SERVER_ERROR to "Internal error"
            else -> HttpStatus.BAD_REQUEST to "Unhandled Aadhaar pattern"
        }

        val retryStatus = when (httpStatus) {
            HttpStatus.OK -> RetryStatus.CLOSED
            HttpStatus.CONFLICT -> RetryStatus.FAILED
            HttpStatus.INTERNAL_SERVER_ERROR -> RetryStatus.OPEN
            else -> RetryStatus.FAILED
        }

        val retryEvent = RetryEvent(
            studentRollNo = event.rollNo,
            taskType = "CBSE_ONBOARDING",
            requestMetadata = mapOf(
                "aadhaar" to event.aadhaar,
                "name" to event.name,
                "rollNo" to event.rollNo,
                "studentClass" to event.studentClass,
                "school" to event.school,
                "dob" to event.dob.toString()
            ),
            responseMetadata = mapOf(
                "status" to httpStatus.value(),
                "message" to responseMessage
            ),
            createdDate = LocalDateTime.now(),
            lastRunDate = LocalDateTime.now(),
            nextRunTime = LocalDateTime.now().plusMinutes(5),
            version = 0,
            status = retryStatus
        )

        retryEventRepository.save(retryEvent).subscribe {
            logger.info("Saved retry event with status: $retryStatus and HTTP ${httpStatus.value()}")
        }
    }
}
