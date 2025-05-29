package com.school.system.service

import com.school.system.dto.StudentOnboardingEvent
import com.school.system.model.RetryEvent
import com.school.system.model.enum.RetryStatus
import com.school.system.repository.RetryEventRepository
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.*

@Service
class RetryEventService(
    private val webClient: WebClient,
    private val retryEventRepository: RetryEventRepository
) {

    fun processStudentOnboarding(event: StudentOnboardingEvent): Mono<Void> {
        val requestBody = mapOf(
            "aadhaar" to event.aadhaar,
            "rollNo" to event.rollNo,
            "name" to event.name,
            "studentClass" to event.studentClass,
            "school" to event.school,
            "dob" to event.dob.toString() // It's "yyyy-MM-dd" - ISO 8601
        )

        return webClient.post()
            .uri("/enroll")
            .bodyValue(requestBody)
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
            .then()
    }
}
