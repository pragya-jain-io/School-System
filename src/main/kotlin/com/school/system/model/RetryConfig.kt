package com.school.system.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "retry_configs")
data class RetryConfig(
    @Id
    val taskType: String,
    val maxRetryCount: Int,
    val retryAfterInMins: Int
)
