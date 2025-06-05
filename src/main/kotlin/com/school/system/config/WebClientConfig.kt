package com.school.system.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * Configuration class to provide a singleton WebClient bean for reactive HTTP calls.
 * WebClient is used for non-blocking, asynchronous REST API calls, making it suitable for reactive applications.
 *
 * This setup centralizes the configuration of the external CBSE API base URL, ensuring all service classes
 * use the same client instance, promoting maintainability and consistency.
 */
@Configuration
class WebClientConfig {

    private val logger = LoggerFactory.getLogger(WebClientConfig::class.java)

    /**
     * Defines a WebClient bean pre-configured with the base URL of the CBSE Mock API.
     * This allows the application to interact with the CBSE API endpoints with consistent settings.
     *
     * @return WebClient instance ready to perform HTTP calls.
     */
    @Bean
    fun webClient(): WebClient = WebClient.builder()
        // Base URL for all outgoing requests using this WebClient
        .baseUrl("http://localhost:8081") // CBSE Mock API URL
        .build()

}
