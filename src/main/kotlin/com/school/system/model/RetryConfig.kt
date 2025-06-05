package com.school.system.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * Data class representing the retry configuration for various task types.
 *
 * This configuration dictates the maximum number of retry attempts allowed
 * and the interval between retries for a specific task.
 */
@Document(collection = "retryConfiguration")
data class RetryConfig(
    @Id val id: String? = null,
    val taskType: String,
    val maxRetryCount: Int,
    val retryAfterInMins: Int
)
