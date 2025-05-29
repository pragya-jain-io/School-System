package com.school.system.controller

import com.school.system.model.Student
import com.school.system.service.StudentService
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/students")
class StudentController(
    private val studentService: StudentService
) {

    @PostMapping
    fun createStudent(@RequestBody student: Student): Mono<Student> {
        return studentService.createStudent(student)
    }

    @GetMapping("/{aadhaar}")
    fun getStudent(@PathVariable aadhaar: String): Mono<Student> {
        return studentService.getStudentByAadhaar(aadhaar)
    }
}
