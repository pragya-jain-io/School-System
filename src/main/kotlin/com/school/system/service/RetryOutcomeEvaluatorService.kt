package com.school.system.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

/**
 * Service responsible for evaluating mock HTTP responses based on input criteria.
 *
 * Currently, this service simulates HTTP status responses by analyzing the last digit
 * of an Aadhaar number. It helps in mocking different outcomes for retry logic
 * in the absence of actual downstream API calls.
 *
 * This is especially useful in development, testing, or event-driven retry workflows.
 */
@Service
class RetryOutcomeEvaluatorService {

    /**
     * Determines a simulated HTTP status code based on the last digit of the Aadhaar number.
     *
     * Mapping logic:
     * - '0' → 200 OK
     * - '1' → 409 CONFLICT
     * - '2' → 500 INTERNAL_SERVER_ERROR
     * - Any other → 400 BAD_REQUEST
     *
     * @param aadhaar The Aadhaar number whose last digit is used for evaluation.
     * @return Corresponding HttpStatus used for retry decision making.
     *
     * Example:
     *  evaluateHttpStatus("123456789012") → HttpStatus.INTERNAL_SERVER_ERROR
     */
    fun evaluateHttpStatus(aadhaar: String): HttpStatus {
        val lastDigit = aadhaar.last()
        return when (lastDigit) {
            '0' -> HttpStatus.OK
            '1' -> HttpStatus.CONFLICT
            '2' -> HttpStatus.INTERNAL_SERVER_ERROR
            else -> HttpStatus.BAD_REQUEST
        }
    }
}