package com.school.system.model

import com.school.system.model.enum.RetryStatus
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import java.util.*

/**
 * Data class representing a retry event stored in MongoDB.
 *
 * This document tracks retry attempts for a given task, such as student onboarding,
 * including request/response metadata, scheduling info, and status.
 */
@Document(collection = "retryEvents")
data class RetryEvent(
    @Id
    val retryId: UUID = UUID.randomUUID(),
    val studentRollNo: String,
    val taskType: String = "CBSE_ONBOARDING",
    val requestMetadata: Map<String, Any>,
    var responseMetadata: Map<String, Any>? = null,
    val createdDate: LocalDateTime = LocalDateTime.now(),
    var lastRunDate: LocalDateTime? = null,
    var nextRunTime: LocalDateTime? = null,
    var version: Int = 0,
    var status: RetryStatus
)