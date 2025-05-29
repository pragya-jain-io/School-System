package com.school.system.repository

import com.school.system.model.RetryConfig
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface RetryConfigRepository : ReactiveMongoRepository<RetryConfig, String>