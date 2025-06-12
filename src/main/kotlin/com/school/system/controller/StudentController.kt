package com.school.system.controller

import com.school.system.model.Student
import com.school.system.service.StudentService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

/**
 * REST controller to manage student-related operations.
 * Handles HTTP requests for creating and retrieving student data.
 */
@RestController // Marks this class as a REST controller, returning JSON responses.
@RequestMapping("/students")    // Base URL for all endpoints in this controller.
class StudentController(
    private val studentService: StudentService  // Dependency-injected service layer for student operations.
) {

    private val logger = LoggerFactory.getLogger(StudentController::class.java)

    /**
     * Endpoint to create a new student.
     * Receives student data in the request body and delegates to the service layer.
     *
     * @param student The student object to be created.
     * @return A Mono emitting ResponseEntity containing the created student.
     *
     * Sample Request:
     * POST /students
     * Body: {
     *   "aadhaar": "012345678902",
     *   "rollNo": "1001",
     *   "name": "Joe",
     *   "studentClass": "10",
     *   "dob": "2012-12-31"
     * }
     */
    @PostMapping
    fun createStudent(@RequestBody student: Student): Mono<ResponseEntity<Student>> {
        logger.info("Received request to create student with Aadhaar: ${student.aadhaar}, RollNo: ${student.rollNo}")
        return studentService.createStudent(student)    // Call service to create the student
            .map { createdStudent ->
                logger.info("Successfully created student with Aadhaar: ${createdStudent.aadhaar}, RollNo: ${createdStudent.rollNo}")
                ResponseEntity.ok(createdStudent) } // Wrap the result in an HTTP 200 OK response
            .doOnError { error ->
                logger.error("Failed to create student with Aadhaar: ${student.aadhaar}", error)
            }
    }


}
