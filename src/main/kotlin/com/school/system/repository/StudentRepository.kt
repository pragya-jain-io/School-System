package com.school.system.repository

import com.school.system.model.Student
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono

interface StudentRepository : ReactiveMongoRepository<Student, String> {
    fun findByRollNo(rollNo: String): Mono<Student>
    fun findByAadhaar(aadhaar: String): Mono<Student>
}