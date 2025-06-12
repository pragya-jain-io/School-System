package com.school.system.controller

import com.school.system.model.RetryEvent
import com.school.system.repository.RetryEventRepository
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Controller responsible for exposing endpoints related to RetryEvent operations.
 *
 * This class provides an API to create RetryEvent documents directly into the database.
 * Useful for manual testing, bootstrapping, or administrative operations.
 *
 * This controller uses reactive programming model via Project Reactor (Mono).
 */
@RestController
@RequestMapping("/retry-events")
class RetryEventController(
    private val retryEventRepository: RetryEventRepository
) {
    private val logger = LoggerFactory.getLogger(RetryEventController::class.java)

    /**
     * API endpoint to create a new RetryEvent.
     *
     * Accepts a RetryEvent object in the request body, and persists it in MongoDB.
     *
     * @param request The RetryEvent object containing all required fields.
     * @return A Mono emitting the saved RetryEvent after persistence.
     *
     * Sample Request (JSON):
     * {
     *   "aadhaar": "099999999902",
     *   "taskType": "CBSE_ONBOARDING",
     *   "requestMetadata": {
     *     "aadhaar": "099999999902",
     *     "name": "Joe",
     *     "rollNo": "1002",
     *     "studentClass": "10",
     *     "school": "ABC Public School",
     *     "dob": "2012-12-31"
     *   },
     *   "responseMetadata": {
     *     "status": 500,
     *     "message": "Internal Server Error"
     *   },
     *   "createdDate": "2025-06-12T07:55:17",
     *   "lastRunDate": "2025-06-12T07:55:17",
     *   "nextRunTime": "2025-06-12T07:56:17",
     *   "version": 0,
     *   "status": "OPEN"
     * }
     */
    @PostMapping
    fun createRetryEvent(@RequestBody request: RetryEvent): Mono<RetryEvent> {
        logger.info("Received request to create RetryEvent for Aadhaar=${request.aadhaar}, taskType=${request.taskType}")

        val retryEvent = RetryEvent(
            aadhaar = request.aadhaar,
            taskType = request.taskType,
            requestMetadata = request.requestMetadata,
            responseMetadata = request.responseMetadata,
            createdDate = request.createdDate,
            lastRunDate = request.lastRunDate,
            nextRunTime = request.nextRunTime,
            version = request.version,
            status = request.status
        )

        return retryEventRepository.save(retryEvent)
            .doOnSuccess {
                logger.info("RetryEvent saved successfully with for Aadhaar=${it.aadhaar}")
            }
            .doOnError { ex ->
                logger.error("Failed to save RetryEvent for Aadhaar=${request.aadhaar}: ${ex.message}", ex)
            }
    }
}
