package com.school.system


import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Main application class for School System.
 *
 * This class serves as the entry point for the Spring Boot application.
 * It bootstraps the entire Spring context and enables key features such as scheduling.
 */
@SpringBootApplication
@EnableScheduling	// Enables Spring's scheduled task execution capability, allowing @Scheduled annotations to work.
class SchoolSystemApplication

/**
 * Kotlin main function to launch the Spring Boot application.
 * Delegates control to Spring Boot's runApplication method which bootstraps the application context.
 */
fun main(args: Array<String>) {
	runApplication<SchoolSystemApplication>(*args)
}
