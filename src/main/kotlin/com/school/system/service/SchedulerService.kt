package com.school.system.service

import com.school.system.model.RetryConfig
import com.school.system.model.enum.RetryStatus
import com.school.system.model.RetryEvent
import com.school.system.repository.RetryConfigRepository
import com.school.system.repository.RetryEventRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime

/**
 * Service responsible for executing scheduled retry logic.
 * Scans for open retry tasks and processes them based on their retry policy.
 */
@Service
class SchedulerService(
    private val retryEventRepository: RetryEventRepository,
    private val retryConfigRepository: RetryConfigRepository
) {

    private val logger = LoggerFactory.getLogger(SchedulerService::class.java)

    /**
     * This method is triggered automatically every 60 seconds using Spring’s @Scheduled.
     * It fetches retryable events, applies retry rules, and updates them accordingly.
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    fun retryOpenTasks() {
        logger.info("Scheduler running at ${LocalDateTime.now()}...")

        // Fetch all retry events that are still OPEN and due for retry
        retryEventRepository.findByStatusAndNextRunTimeBefore(RetryStatus.OPEN, LocalDateTime.now())
            // Log each matching event for visibility
            .doOnNext { logger.info("Found task to retry for student: ${it.studentRollNo}, version: ${it.version}") }
            // For each retry event, fetch its retry configuration
            .flatMap { event ->
                retryConfigRepository.findByTaskType(event.taskType)
                    // Log retry config values
                    .doOnNext { logger.info("Loaded retry config for taskType=${event.taskType}, maxRetry=${it.maxRetryCount}, retryAfter=${it.retryAfterInMins} mins") }
                    // If no retry config found, skip the event with a warning
                    .switchIfEmpty(
                    Mono.defer {
                        logger.warn("No retry config found for taskType=${event.taskType}. Skipping retry.")
                        Mono.empty<RetryConfig>() // specify the type explicitly here
                    }
                )
                    // Process the retry event using fetched config
                    .flatMap { config -> processRetry(event, config.maxRetryCount, config.retryAfterInMins) }
            }
            // Subscribe to start the reactive stream and log the result
            .subscribe(
                { updated -> logger.info("Retried: ${updated.studentRollNo}, Status: ${updated.status}") },
                { error -> logger.error("Error in retry scheduler: ", error) }
            )
    }

    /**
     * Core retry logic applied to each eligible retry event.
     * Simulates an external system call based on Aadhaar’s last digit
     * and updates the RetryEvent accordingly.
     */
    private fun processRetry(event: RetryEvent, maxRetry: Int, retryAfterMins: Int): Mono<RetryEvent> {
        // Check if attempt to retry has already reached the configured maximum
        if (event.version >= maxRetry) {
            logger.info("Max retries reached for ${event.studentRollNo}")
            return Mono.empty()
        }

        // Simulated HTTP status decision based on Aadhaar last digit
        val aadhaarValue = event.requestMetadata["aadhaar"]?.toString()
        val aadhaarLastDigit = aadhaarValue?.takeLast(1)?.firstOrNull()
        val (httpStatus, message) = when (aadhaarLastDigit) {
            '0' -> HttpStatus.OK to "Student enrolled successfully"
            '1' -> HttpStatus.CONFLICT to "Student already enrolled"
            '2' -> HttpStatus.INTERNAL_SERVER_ERROR to "Internal error"
            else -> HttpStatus.BAD_REQUEST to "Unhandled Aadhaar pattern"
        }
        logger.info("Processing Aadhaar ending with '$aadhaarLastDigit' for student: ${event.studentRollNo}")

        // Determine new retry status based on the simulated response
        val newStatus = when (httpStatus) {
            HttpStatus.OK -> RetryStatus.CLOSED
            HttpStatus.CONFLICT -> RetryStatus.FAILED
            HttpStatus.INTERNAL_SERVER_ERROR ->
                if (event.version + 1 >= maxRetry) RetryStatus.FAILED else RetryStatus.OPEN
            else -> RetryStatus.FAILED
        }

        // Log detailed retry update info
        logger.info("Updating RetryEvent for student: ${event.studentRollNo} | HTTP: $httpStatus | Message: $message | Next Retry: ${LocalDateTime.now().plusMinutes(retryAfterMins.toLong())} | New Status: $newStatus")

        // Create an updated copy of the event with new status, version, and timing
        val updated = event.copy(
            responseMetadata = mapOf("status" to httpStatus.value(), "message" to message),
            lastRunDate = LocalDateTime.now(),
            nextRunTime = LocalDateTime.now().plusMinutes(retryAfterMins.toLong()),
            status = newStatus,
            version = event.version + 1
        )

        // Save updated retry event back to the repository
        return retryEventRepository.save(updated)
    }
}
