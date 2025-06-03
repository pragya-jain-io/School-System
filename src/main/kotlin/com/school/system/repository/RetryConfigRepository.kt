package com.school.system.repository

import com.school.system.model.RetryConfig
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono

interface RetryConfigRepository : ReactiveMongoRepository<RetryConfig, String>{
    fun findByTaskType(taskType: String): Mono<RetryConfig>
}