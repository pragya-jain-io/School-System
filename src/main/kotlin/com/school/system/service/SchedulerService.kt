package com.school.system.service

import com.school.system.model.enum.RetryStatus
import com.school.system.repository.RetryConfigRepository
import com.school.system.repository.RetryEventRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime

/**
 * SchedulerService is responsible for managing and executing retry logic at regular intervals.
 * It scans the database for retryable events and processes them according to their retry configuration.
 */
@Service
class SchedulerService(
    private val retryEventRepository: RetryEventRepository,
    private val retryConfigRepository: RetryConfigRepository,
    private val processor: RetryEventProcessorService
) {
    private val logger = LoggerFactory.getLogger(SchedulerService::class.java)

    /**
     * Scheduled method that runs every 60 seconds (fixedRate = 60000 ms).
     * It processes all RetryEvents with status OPEN and scheduled before the current time.
     */
    @Scheduled(fixedRate = 120000)
    fun retryOpenTasks() {
        logger.info("Scheduler running at ${LocalDateTime.now()}")

        // Fetch retry events that are eligible for retry
        retryEventRepository.findByStatusAndNextRunTimeBefore(RetryStatus.OPEN, LocalDateTime.now())

            // Log each retry event being processed
            .doOnNext { logger.info("Retrying student: ${it.aadhaar}, version: ${it.version}") }

            // For each retry event, fetch its configuration and process
            .flatMap { event ->
                retryConfigRepository.findByTaskType(event.taskType)
                    // If no config is found, skip the event
                    .switchIfEmpty(Mono.defer {
                        logger.warn("No config for taskType=${event.taskType}. Skipping.")
                        Mono.empty()
                    })

                    // Process retry logic with the found config
                    .flatMap { config ->
                        // Extract aadhaar from request metadata; if not present, skip
                        val aadhaar = event.requestMetadata["aadhaar"]?.toString() ?: return@flatMap Mono.empty()
                        // Determine the outcome of retry using business logic
                        val (httpStatus, retryStatus) = processor.evaluateRetryOutcome(aadhaar)

                        // Check if max retries have been reached
                        if (event.version >= config.maxRetryCount) {
                            logger.info("Max retry attempts reached for ${event.aadhaar}. Marking as FAILED.")

                            val failedEvent = event.copy(
                                status = RetryStatus.FAILED,
                                lastRunDate = LocalDateTime.now(),
                                responseMetadata = mapOf("message" to "Max retries reached")
                            )

                            return@flatMap retryEventRepository.save(failedEvent)
                        }

                        // Otherwise, update retry event with new response and timing info
                        val updated = processor.updateRetryEvent(event, config.retryAfterInMins, httpStatus, retryStatus)
                        retryEventRepository.save(updated)
                    }
            }
            // Subscribe to the reactive stream to trigger execution
            .subscribe(
                { logger.info("Retry processed for student: ${it.aadhaar} | Status: ${it.status}") },
                { logger.error("Error during retry: ", it) }
            )
    }
}
