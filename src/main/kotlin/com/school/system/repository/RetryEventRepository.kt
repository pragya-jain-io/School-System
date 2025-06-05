package com.school.system.repository

import com.school.system.model.RetryEvent
import com.school.system.model.enum.RetryStatus
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.*

/**
 * Repository interface for managing RetryEvent documents in MongoDB.
 *
 * Extends ReactiveMongoRepository providing reactive CRUD operations on RetryEvent objects.
 *
 * UUID is the type of the ID field for RetryEvent documents.
 */

interface RetryEventRepository : ReactiveMongoRepository<RetryEvent, UUID> {

    /**
     * Custom query method to find all RetryEvents that are in a specific status
     * and have a nextRunTime before the given LocalDateTime.
     *
     * This is used to fetch retry tasks that are due to run at or before the current time.
     *
     * @param status The RetryStatus to filter events (e.g., OPEN, FAILED).
     * @param time The cutoff LocalDateTime to find events scheduled to run before this time.
     * @return A Flux stream of RetryEvents matching the criteria.
     */
    fun findByStatusAndNextRunTimeBefore(
        status: RetryStatus,
        time: LocalDateTime
    ): Flux<RetryEvent>


    /**
     * Retrieves a single RetryEvent by Aadhaar number and task type.
     *
     * This is a key method for implementing idempotency—ensures no duplicate retry events are created
     * for the same task and same entity (in this case, a student).
     *
     * @param aadhaar Unique student identifier used as part of the idempotency key.
     * @param taskType Logical task category (e.g., "CBSE_ONBOARDING").
     * @return A Mono containing the found RetryEvent or empty if none exists.
     */
    fun findByAadhaarAndTaskType(aadhaar: String, taskType: String): Mono<RetryEvent?>
}