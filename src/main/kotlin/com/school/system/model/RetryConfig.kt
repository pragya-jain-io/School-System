package com.school.system.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "retryConfiguration")
data class RetryConfig(
    @Id val id: String? = null,
    val taskType: String,
    val maxRetryCount: Int,
    val retryAfterInMins: Int
)
