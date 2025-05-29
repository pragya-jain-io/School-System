package com.school.system.repository

import com.school.system.model.RetryEvent
import com.school.system.model.enum.RetryStatus
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import java.time.LocalDateTime
import java.util.*

interface RetryEventRepository : ReactiveMongoRepository<RetryEvent, UUID> {
    fun findByStatusAndNextRunTimeBefore(
        status: RetryStatus,
        time: LocalDateTime
    ): Flux<RetryEvent>
}