package com.school.system.consumer

import com.school.system.dto.StudentOnboardingEvent
import com.school.system.repository.RetryEventRepository
import com.school.system.service.RetryEventProcessorService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

/**
 * Kafka consumer that listens to the "student-onboarding" topic.
 * For each received StudentOnboardingEvent, it conditionally creates a RetryEvent
 * based on whether it already exists, and handles it according to configured retry logic.
 */
@Component
class StudentOnboardingConsumer(
    private val retryEventRepository: RetryEventRepository,
    private val processor: RetryEventProcessorService
) {
    private val logger = LoggerFactory.getLogger(StudentOnboardingConsumer::class.java)

    /**
     * This function is triggered automatically when a new message arrives on the "student-onboarding" Kafka topic.
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
                // If no existing event is found, prepare metadata for creating a new RetryEvent
                val metadata = mapOf(
                    "aadhaar" to event.aadhaar,
                    "name" to event.name,
                    "rollNo" to event.rollNo,
                    "studentClass" to event.studentClass,
                    "school" to event.school,
                    "dob" to event.dob.toString()
                )

                // Use business logic to evaluate outcome (HTTP and RetryStatus) based on Aadhaar last digit
                val (httpStatus, retryStatus) = processor.evaluateRetryOutcome(event.aadhaar)
                val retryEvent = processor.buildInitialRetryEvent(event.aadhaar, metadata, httpStatus, retryStatus)

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
}
