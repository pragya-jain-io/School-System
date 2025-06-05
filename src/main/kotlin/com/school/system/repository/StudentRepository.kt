package com.school.system.repository

import com.school.system.model.Student
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

/**
 * Reactive repository interface for Student entities.
 *
 * Extends ReactiveMongoRepository to provide reactive CRUD operations.
 * The primary key type for Student is String (likely the student's Aadhaar or RollNo).
 */
interface StudentRepository : ReactiveMongoRepository<Student, String> {}