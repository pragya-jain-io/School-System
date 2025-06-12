package com.school.system.consumer

import com.school.system.dto.StudentOnboardingEvent
import com.school.system.model.RetryEvent
import com.school.system.model.enum.RetryStatus
import com.school.system.repository.RetryEventRepository
import com.school.system.service.RetryOutcomeEvaluatorService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono
import java.time.LocalDateTime

/**
 * Kafka consumer that listens to the "student-onboarding" topic.
 * For each received StudentOnboardingEvent, it conditionally creates a RetryEvent
 * based on whether it already exists, and handles it according to configured retry logic.
 */
@Component
class StudentOnboardingConsumer(
    private val retryEventRepository: RetryEventRepository,
    private val retryOutcomeEvaluator: RetryOutcomeEvaluatorService
) {
    private val logger = LoggerFactory.getLogger(StudentOnboardingConsumer::class.java)

    /**
     * This function is triggered automatically when a new message arrives at the "student-onboarding" Kafka topic.
     * It either creates a new RetryEvent or skips if one already exists.
     */
    @KafkaListener(
        topics = ["student-onboarding"],
        groupId = "school-service",
        containerFactory = "kafkaListenerFactory"
    )
    fun listen(event: StudentOnboardingEvent) {
        // Check if a RetryEvent already exists for the given Aadhaar and task type
        retryEventRepository.findByAadhaarAndTaskType(event.aadhaar, "CBSE_ONBOARDING")
            .switchIfEmpty(Mono.defer {

                // Use business logic to evaluate outcome (HTTP and RetryStatus) based on Aadhaar last digit
                val httpStatus = retryOutcomeEvaluator.evaluateHttpStatus(event.aadhaar)
                val retryStatus = mapHttpToRetryStatus(httpStatus)

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
                    "message" to httpStatus
                ),
                createdDate = LocalDateTime.now(),
                lastRunDate = LocalDateTime.now(),
                nextRunTime = LocalDateTime.now().plusMinutes(1),
                version = 0,
                status = retryStatus
            )


                // Save the new RetryEvent to MongoDB
                retryEventRepository.save(retryEvent)
                    .doOnSuccess {
                        logger.info("RetryEvent saved for Aadhaar=${event.aadhaar} with status=$retryStatus")
                    }
                    .subscribe()
                Mono.empty()
            })
            // If the event already exists, log and skip further processing
            .subscribe {
                logger.info("Duplicate event for Aadhaar='${event.aadhaar}'. Skipping.")
            }
    }

    /**
     * Maps simulated HTTP status to internal RetryStatus.
     *
     * @param httpStatus The simulated HTTP status derived from business logic.
     * @return The mapped RetryStatus used for retry event persistence.
     */
    private fun mapHttpToRetryStatus(httpStatus: HttpStatus): RetryStatus {
        return when (httpStatus) {
            HttpStatus.OK -> RetryStatus.CLOSED
            HttpStatus.CONFLICT -> RetryStatus.FAILED
            HttpStatus.INTERNAL_SERVER_ERROR -> RetryStatus.OPEN
            else -> RetryStatus.FAILED
        }
    }

}
