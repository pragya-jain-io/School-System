package com.school.system.consumer

import com.school.system.dto.StudentOnboardingEvent
import com.school.system.service.RetryEventService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class StudentOnboardingConsumer(
    private val retryEventService: RetryEventService
) {

    @KafkaListener(
        topics = ["student-onboarding"],
        groupId = "school-service",
        containerFactory = "kafkaListenerFactory"
    )
    fun consume(event: StudentOnboardingEvent) {
        retryEventService.processStudentOnboarding(event)
            .subscribe()
    }
}
