package com.school.system.consumer

import com.school.system.dto.StudentOnboardingEvent
import com.school.system.model.RetryEvent
import com.school.system.model.enum.RetryStatus
import com.school.system.repository.RetryEventRepository
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono
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
        // Check if a retry event already exists for this Aadhaar and task type to ensure idempotency.
        retryEventRepository.findByAadhaarAndTaskType(event.aadhaar, "CBSE_ONBOARDING")
            .switchIfEmpty(Mono.defer {
                // No prior retry found – proceed with processing this onboarding event.
                logger.info("No existing RetryEvent found for Aadhaar='${event.aadhaar}'. Proceeding to process.")
                processEvent(event)
                Mono.empty()
            })
            .subscribe {
                // Retry already exists – skip duplicate processing.
                logger.info("Duplicate event for Aadhaar='${event.aadhaar}'. Skipping.")
            }
    }

    /**
     * Processes the student onboarding event by simulating an API outcome based on Aadhaar last digit.
     * Determines retry status and saves a new RetryEvent in the database.
     */
    private fun processEvent(event: StudentOnboardingEvent) {
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
            aadhaar = event.aadhaar,
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
            nextRunTime = LocalDateTime.now().plusMinutes(1),
            version = 0,
            status = retryStatus
        )

        // Save the retry event and log outcome once persistence is complete.
        retryEventRepository.save(retryEvent).subscribe {
            logger.info("RetryEvent saved successfully with status='$retryStatus' and HTTP status=${httpStatus.value()}")
        }
    }
}
