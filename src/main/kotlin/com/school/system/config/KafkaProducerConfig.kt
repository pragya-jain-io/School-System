package com.school.system.config

import com.school.system.dto.StudentOnboardingEvent
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

/**
 * Spring Configuration class responsible for setting up Kafka producer beans.
 * It configures the connection to Kafka brokers, serialization mechanisms,
 * and exposes a KafkaTemplate to send messages of type StudentOnboardingEvent.
 */
@Configuration
class KafkaProducerConfig {

    private val logger = LoggerFactory.getLogger(KafkaProducerConfig::class.java)

    /**
     * Defines a ProducerFactory bean responsible for creating Kafka producers.
     * This factory sets up all necessary producer configs like bootstrap servers
     * and key/value serializers for sending messages.
     *
     * @return ProducerFactory<String, StudentOnboardingEvent> configured for our use case.
     */
    @Bean
    fun producerFactory(): ProducerFactory<String, StudentOnboardingEvent> {

        val bootstrapServers = "localhost:9092"
        logger.info("Setting up Kafka Producer Factory with broker: $bootstrapServers")

        // Map of producer configuration properties
        val config = mapOf(
            // Kafka broker address for the producer to connect
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
            // Serializer class for message keys (serialized as String)
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            // Serializer class for message values (serialized as JSON)
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java
        )
        // Return a DefaultKafkaProducerFactory with the above config
        return DefaultKafkaProducerFactory(config)
    }

    /**
     * Defines a KafkaTemplate bean that acts as the main entry point for producing messages.
     * KafkaTemplate simplifies sending data to Kafka topics by providing helper methods.
     *
     * @return KafkaTemplate<String, StudentOnboardingEvent> for sending messages with String keys.
     */
    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, StudentOnboardingEvent> {
        // Create KafkaTemplate using the configured producer factory
        return KafkaTemplate(producerFactory())
    }
}
