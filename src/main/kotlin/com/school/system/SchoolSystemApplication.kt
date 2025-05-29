package com.school.system

import com.school.system.repository.StudentRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class SchoolSystemApplication
fun main(args: Array<String>) {
	runApplication<SchoolSystemApplication>(*args)
}
