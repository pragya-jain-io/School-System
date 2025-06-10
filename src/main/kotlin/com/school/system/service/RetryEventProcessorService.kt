package com.school.system.service

import com.school.system.model.enum.RetryStatus
import com.school.system.model.RetryEvent
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Service class that encapsulates the logic for:
 * - Determining the retry outcome based on business rules (e.g., Aadhaar pattern)
 * - Constructing new RetryEvent objects for initial processing
 * - Updating existing RetryEvent objects during retry attempts
 *
 * This promotes reusability and separation of concerns between event handling and processing logic.
 */
@Service
class RetryEventProcessorService {
    private val logger = LoggerFactory.getLogger(RetryEventProcessorService::class.java)

    /**
     * Evaluates the Aadhaar number's last digit to determine:
     * - Corresponding HTTP status (mock logic)
     * - Corresponding RetryStatus to persist
     *
     * @param aadhaar Aadhaar number of the student
     * @return Pair of HttpStatus and RetryStatus
     */
    fun evaluateRetryOutcome(aadhaar: String): Pair<HttpStatus, RetryStatus> {
        val lastDigit = aadhaar.last()

        // Determine HTTP status based on last digit (mocked decision logic)
        val httpStatus = when (lastDigit) {
            '0' -> HttpStatus.OK
            '1' -> HttpStatus.CONFLICT
            '2' -> HttpStatus.INTERNAL_SERVER_ERROR
            else -> HttpStatus.BAD_REQUEST
        }

        // Map HTTP status to retry status
        val retryStatus = when (httpStatus) {
            HttpStatus.OK -> RetryStatus.CLOSED
            HttpStatus.CONFLICT -> RetryStatus.FAILED
            HttpStatus.INTERNAL_SERVER_ERROR -> RetryStatus.OPEN
            else -> RetryStatus.FAILED
        }

        return httpStatus to retryStatus
    }

    /**
     * Builds and returns a new RetryEvent instance for a fresh onboarding event.
     * This is used when no previous retry record exists for this Aadhaar.
     *
     * @param aadhaar Unique Aadhaar number for the student
     * @param metadata Map containing original student details (name, rollNo, etc.)
     * @param httpStatus Resulting HTTP status based on business logic
     * @param retryStatus Resulting retry status to persist
     * @return Fully populated RetryEvent instance
     */
    fun buildInitialRetryEvent(
        aadhaar: String,
        metadata: Map<String, Any>,
        httpStatus: HttpStatus,
        retryStatus: RetryStatus
    ): RetryEvent {
        return RetryEvent(
            aadhaar = aadhaar,
            taskType = "CBSE_ONBOARDING",
            requestMetadata = metadata,
            responseMetadata = mapOf(
                "status" to httpStatus.value(),
                "message" to getResponseMessage(httpStatus)
            ),
            createdDate = LocalDateTime.now(),
            lastRunDate = LocalDateTime.now(),
            nextRunTime = LocalDateTime.now().plusMinutes(1),
            version = 0,
            status = retryStatus
        )
    }

    /**
     * Updates an existing RetryEvent after a failed attempt that is being retried.
     *
     * @param event The existing RetryEvent from DB
     * @param retryAfterMins Number of minutes to wait before next retry
     * @param httpStatus New status based on the retry logic
     * @param retryStatus New retry lifecycle status
     * @return A new RetryEvent copy with updated values
     */
    fun updateRetryEvent(
        event: RetryEvent,
        retryAfterMins: Int,
        httpStatus: HttpStatus,
        retryStatus: RetryStatus
    ): RetryEvent {
        return event.copy(
            responseMetadata = mapOf(
                "status" to httpStatus.value(),
                "message" to getResponseMessage(httpStatus)
            ),
            lastRunDate = LocalDateTime.now(),
            nextRunTime = LocalDateTime.now().plusMinutes(retryAfterMins.toLong()),
            version = event.version + 1,
            status = retryStatus
        )
    }

    /**
     * Maps HttpStatus to a meaningful message.
     * Helps keep message mapping consistent across methods.
     *
     * @param httpStatus Status used to generate message
     * @return Corresponding description string
     */
    private fun getResponseMessage(httpStatus: HttpStatus): String = when (httpStatus) {
        HttpStatus.OK -> "Student enrolled successfully"
        HttpStatus.CONFLICT -> "Student already enrolled"
        HttpStatus.INTERNAL_SERVER_ERROR -> "Internal error"
        else -> "Unhandled Aadhaar pattern"
    }
}