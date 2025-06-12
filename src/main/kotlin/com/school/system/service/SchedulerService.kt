package com.school.system.service

import com.school.system.model.enum.RetryStatus
import com.school.system.repository.RetryConfigRepository
import com.school.system.repository.RetryEventRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime

/**
 * SchedulerService is a background service responsible for processing retryable events
 * based on configurable rules. It runs periodically and updates the retry event status
 * depending on the outcome of each retry attempt.
 */
@Service
class SchedulerService(
    private val retryEventRepository: RetryEventRepository,
    private val retryConfigRepository: RetryConfigRepository,
    private val retryOutcomeEvaluator: RetryOutcomeEvaluatorService
) {
    private val logger = LoggerFactory.getLogger(SchedulerService::class.java)

    /**
     * Executes every 60 seconds to process events that are eligible for retry.
     * It queries for retry events with status OPEN and `nextRunTime` in the past,
     * applies business logic to determine the retry outcome, and updates the event accordingly.
     */
    @Scheduled(fixedRate = 60000)
    fun retryOpenTasks() {
        logger.info("Scheduler running at ${LocalDateTime.now()}")

        // Fetch retry events that are eligible for retry
        retryEventRepository.findByStatusAndNextRunTimeBefore(RetryStatus.OPEN, LocalDateTime.now())

            // Log each retry event being processed
            .doOnNext { logger.info("Retrying student: ${it.aadhaar}, version: ${it.version}") }

            // For each retry event, fetch its configuration and process
            .flatMap { event ->
                retryConfigRepository.findByTaskType(event.taskType)
                    .switchIfEmpty(Mono.defer {
                        logger.warn("No config found for taskType=${event.taskType}. Skipping.")
                        Mono.empty()
                    })
                    .flatMap { config ->
                        // 1. Extract Aadhaar from request metadata
                        val aadhaar = event.requestMetadata["aadhaar"]?.toString()
                            ?: return@flatMap Mono.empty()

                        // 3. Simulate HTTP response based on last digit
                        val httpStatus = retryOutcomeEvaluator.evaluateHttpStatus(aadhaar)
                        logger.info(
                            """
                            Mocked: 
                            HTTP: $httpStatus
                            Aadhaar: $aadhaar
                            """.trimIndent()
                        )

                        // 4. Determine retry status based on HTTP response and retry count
                        val retryStatus = when (httpStatus) {
                            HttpStatus.OK -> RetryStatus.CLOSED
                            HttpStatus.CONFLICT -> RetryStatus.FAILED
                            HttpStatus.INTERNAL_SERVER_ERROR -> {
                                if (event.version+1 >= config.maxRetryCount) RetryStatus.FAILED
                                else RetryStatus.OPEN
                            }
                            else -> RetryStatus.FAILED
                        }

                        logger.info(
                            """
                            Updating retry event:
                            Student: ${event.aadhaar}
                            HTTP: $httpStatus
                            New Status: $retryStatus
                            Retry Count: ${event.version + 1}/${config.maxRetryCount}
                            Next Retry: ${LocalDateTime.now().plusMinutes(config.retryAfterInMins.toLong())}
                            """.trimIndent()
                        )

                        // 5. Prepare updated RetryEvent
                        val updatedEvent = event.copy(
                            responseMetadata = mapOf(
                                "status" to httpStatus.value(),
                                "message" to httpStatus.reasonPhrase // This simulates actual HTTP response info
                            ),
                            lastRunDate = LocalDateTime.now(),
                            nextRunTime = LocalDateTime.now().plusMinutes(config.retryAfterInMins.toLong()),
                            version = event.version + 1,
                            status = retryStatus
                        )

                        retryEventRepository.save(updatedEvent)
                    }
            }

            // Subscribe to the reactive stream to trigger execution
            .subscribe(
                { logger.info("Retry processed for student: ${it.aadhaar} | Status: ${it.status}") },
                { logger.error("Error during retry: ", it) }
            )
    }
}
