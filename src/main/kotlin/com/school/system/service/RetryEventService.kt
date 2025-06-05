package com.school.system.service

import com.school.system.dto.StudentOnboardingEvent
import com.school.system.model.RetryEvent
import com.school.system.model.enum.RetryStatus
import com.school.system.repository.RetryEventRepository
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.LocalDateTime

/**
 * Service responsible for processing onboarding events and managing retry records.
 * It encapsulates business logic around sending onboarding requests and recording their outcomes.
 */
@Service
class RetryEventService(
    private val webClient: WebClient, // WebClient is used for making non-blocking HTTP calls
    private val retryEventRepository: RetryEventRepository  // Repository to persist RetryEvent data
) {

    /**
     * Processes an incoming onboarding event by:
     * 1. Calling the /enroll endpoint
     * 2. Mapping the response status to RetryStatus
     * 3. Creating and saving a RetryEvent for auditing or retry purposes
     *
     * @param event the onboarding event received from Kafka
     * @return Mono<Void> - completes when processing is done
     */
    fun processStudentOnboarding(event: StudentOnboardingEvent): Mono<Void> {
        // Constructing the request payload from the event
        val requestBody = mapOf(
            "aadhaar" to event.aadhaar,
            "rollNo" to event.rollNo,
            "name" to event.name,
            "studentClass" to event.studentClass,
            "school" to event.school,
            "dob" to event.dob.toString() // It's "yyyy-MM-dd" - ISO 8601
        )

        return webClient.post()
            .uri("/enroll") // External POST API to enroll a student
            .bodyValue(requestBody) // Setting the request body
            .retrieve()
            .toBodilessEntity()
            .map { response ->
                val status = when (response.statusCode.value()) {
                    200 -> RetryStatus.CLOSED
                    409 -> RetryStatus.FAILED
                    else -> RetryStatus.OPEN
                }

                RetryEvent(
                    studentRollNo = event.rollNo,
                    requestMetadata = requestBody,
                    responseMetadata = mapOf("statusCode" to response.statusCode.value()),
                    createdDate = LocalDateTime.now(),
                    lastRunDate = LocalDateTime.now(),
                    nextRunTime = LocalDateTime.now().plusMinutes(10),
                    version = 0,
                    status = status
                )
            }
            .flatMap { retryEventRepository.save(it) }
            .then() // Mono<Void> returned to signal completion without value
    }
}
