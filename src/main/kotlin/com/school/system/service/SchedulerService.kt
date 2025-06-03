package com.school.system.service

import com.school.system.model.RetryConfig
import com.school.system.model.enum.RetryStatus
import com.school.system.model.RetryEvent
import com.school.  system.repository.RetryConfigRepository
import com.school.system.repository.RetryEventRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
class SchedulerService(
    private val retryEventRepository: RetryEventRepository,
    private val retryConfigRepository: RetryConfigRepository
) {

    private val logger = LoggerFactory.getLogger(SchedulerService::class.java)

    @Scheduled(fixedRate = 60000) // Every 60 seconds
    fun retryOpenTasks() {
        logger.info("Scheduler running at ${LocalDateTime.now()}...")

        retryEventRepository.findByStatusAndNextRunTimeBefore(RetryStatus.OPEN, LocalDateTime.now())
            .doOnNext { logger.info("Found task to retry for student: ${it.studentRollNo}, version: ${it.version}") }
            .flatMap { event ->
                retryConfigRepository.findByTaskType(event.taskType)
                    .doOnNext { logger.info("Loaded retry config for taskType=${event.taskType}, maxRetry=${it.maxRetryCount}, retryAfter=${it.retryAfterInMins} mins") }
                    .switchIfEmpty(
                    Mono.defer {
                        logger.warn("⚠️ No retry config found for taskType=${event.taskType}. Skipping retry.")
                        Mono.empty<RetryConfig>() // specify the type explicitly here
                    }
                )
                    .flatMap { config -> processRetry(event, config.maxRetryCount, config.retryAfterInMins) }
            }
            .subscribe(
                { updated -> logger.info("Retried: ${updated.studentRollNo}, Status: ${updated.status}") },
                { error -> logger.error("Error in retry scheduler: ", error) }
            )
    }

    private fun processRetry(event: RetryEvent, maxRetry: Int, retryAfterMins: Int): Mono<RetryEvent> {
        if (event.version >= maxRetry) {
            logger.info("Max retries reached for ${event.studentRollNo}")
            return Mono.empty()
        }

        val aadhaarValue = event.requestMetadata["aadhaar"]?.toString()
        val aadhaarLastDigit = aadhaarValue?.takeLast(1)?.firstOrNull()
        val (httpStatus, message) = when (aadhaarLastDigit) {
            '0' -> HttpStatus.OK to "Student enrolled successfully"
            '1' -> HttpStatus.CONFLICT to "Student already enrolled"
            '2' -> HttpStatus.INTERNAL_SERVER_ERROR to "Internal error"
            else -> HttpStatus.BAD_REQUEST to "Unhandled Aadhaar pattern"
        }
        logger.info("Processing Aadhaar ending with '$aadhaarLastDigit' for student: ${event.studentRollNo}")
        val newStatus = when (httpStatus) {
            HttpStatus.OK -> RetryStatus.CLOSED
            HttpStatus.CONFLICT -> RetryStatus.FAILED
            HttpStatus.INTERNAL_SERVER_ERROR ->
                if (event.version + 1 >= maxRetry) RetryStatus.FAILED else RetryStatus.OPEN
            else -> RetryStatus.FAILED
        }

        logger.info("Updating RetryEvent for student: ${event.studentRollNo} | HTTP: $httpStatus | Message: $message | Next Retry: ${LocalDateTime.now().plusMinutes(retryAfterMins.toLong())} | New Status: $newStatus")
        val updated = event.copy(
            responseMetadata = mapOf("status" to httpStatus.value(), "message" to message),
            lastRunDate = LocalDateTime.now(),
            nextRunTime = LocalDateTime.now().plusMinutes(retryAfterMins.toLong()),
            status = newStatus,
            version = event.version + 1
        )

        return retryEventRepository.save(updated)
    }
}
