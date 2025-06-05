package com.school.system.repository

import com.school.system.model.RetryConfig
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono

/**
 * Reactive repository interface for managing RetryConfig documents in MongoDB.
 *
 * Extends ReactiveMongoRepository to leverage reactive, non-blocking CRUD operations.
 * The primary key for RetryConfig is of type String (MongoDB ObjectId).
 */
interface RetryConfigRepository : ReactiveMongoRepository<RetryConfig, String>{

    /**
     * Custom query method to retrieve a RetryConfig by its taskType field.
     *
     * @param taskType the identifier of the task (e.g., "CBSE_ONBOARDING").
     * @return a Mono emitting the matching RetryConfig or empty if none found.
     *
     * This method follows Spring Data's naming conventions to automatically generate
     * the underlying MongoDB query.
     */
    fun findByTaskType(taskType: String): Mono<RetryConfig>
}