package com.school.system.consumer

import com.school.system.dto.StudentOnboardingEvent
import com.school.system.model.RetryEvent
import com.school.system.model.enum.RetryStatus
import com.school.system.repository.RetryEventRepository
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

/**
 * Kafka consumer that listens to student onboarding events and saves a RetryEvent
 * based on the outcome determined from Aadhaar logic.
 */
@Component  // Marks this class as a Spring-managed component eligible for Kafka event handling.
class StudentOnboardingConsumer(
    private val retryEventRepository: RetryEventRepository
) {
    private val logger = LoggerFactory.getLogger(StudentOnboardingConsumer::class.java)

    /**
     * Listens to the Kafka topic "student-onboarding" and handles incoming StudentOnboardingEvent messages.
     * Deserialization and listener container setup is handled via kafkaListenerFactory.
     */
    @KafkaListener(
        topics = ["student-onboarding"],
        groupId = "school-service",
        containerFactory = "kafkaListenerFactory"
    )
    fun listen(event: StudentOnboardingEvent) {
        logger.info("Consumed Kafka event for student rollNo='${event.rollNo}'")

        // Extract the last digit of the Aadhaar number to simulate API behavior.
        val aadhaarLastDigit = event.aadhaar.last()

        // Simulated API response logic based on Aadhaar last digit
        val (httpStatus, responseMessage) = when (aadhaarLastDigit) {
            '0' -> HttpStatus.OK to "Student enrolled successfully"
            '1' -> HttpStatus.CONFLICT to "Student already enrolled"
            '2' -> HttpStatus.INTERNAL_SERVER_ERROR to "Internal error"
            else -> HttpStatus.BAD_REQUEST to "Unhandled Aadhaar pattern"
        }

        // Determine retry status based on simulated response
        val retryStatus = when (httpStatus) {
            HttpStatus.OK -> RetryStatus.CLOSED
            HttpStatus.CONFLICT -> RetryStatus.FAILED
            HttpStatus.INTERNAL_SERVER_ERROR -> RetryStatus.OPEN
            else -> RetryStatus.FAILED
        }

        // Create a new RetryEvent object to persist the retry state and metadata
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
            responseMetadata = mapOf(   // Save simulated response info
                "status" to httpStatus.value(),
                "message" to responseMessage
            ),
            createdDate = LocalDateTime.now(),
            lastRunDate = LocalDateTime.now(),
            nextRunTime = LocalDateTime.now().plusMinutes(5),   // Schedule next retry (if needed) 5 mins later
            version = 0, // Retry version starts at 0
            status = retryStatus
        )

        // Save the retry event asynchronously and log success
        retryEventRepository.save(retryEvent).subscribe {
            logger.info("RetryEvent saved successfully with status='$retryStatus' and HTTP status=${httpStatus.value()}")
        }
    }
}
