package com.school.system.model.enum

/**
 * Enum representing the possible states of a retry event.
 *
 * - OPEN: The retry task is pending and should be retried.
 * - CLOSED: The retry task completed successfully or is no longer needed.
 * - FAILED: The retry task failed permanently after max attempts or encountered unrecoverable errors.
 */
enum class RetryStatus {
    OPEN,
    CLOSED,
    FAILED
}