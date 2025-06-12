package com.school.system.config

import com.school.system.dto.StudentOnboardingEvent
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer

/**
 * Enables Kafka listener annotations and declares this class as a Spring configuration.
 * This class configures how the application consumes Kafka messages for StudentOnboardingEvent.
 */
@EnableKafka    // Enables Kafka listener infrastructure within Spring Boot
@Configuration  // Marks this class as a configuration class, so Spring Boot reads it and applies the beans defined inside.
class KafkaConsumerConfig {

    private val logger = LoggerFactory.getLogger(KafkaConsumerConfig::class.java)
    /**
     * Defines the ConsumerFactory bean responsible for creating Kafka consumers.
     * This factory sets up all necessary consumer configs like bootstrap servers,
     * group ID, key/value deserializers, and trusted packages for JSON deserialization.
     *
     * @return ConsumerFactory<String, StudentOnboardingEvent> customized for our data type.
     */
    @Bean
    fun consumerFactory(): ConsumerFactory<String, StudentOnboardingEvent> {

        val bootstrapServers = "localhost:9092"
        val groupId = "school-service"

        logger.info("Configuring Kafka Consumer Factory with broker: $bootstrapServers and group: $groupId")

        // Consumer configuration properties map
        val config = mapOf(
            // Kafka broker address to connect consumers to
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
            // Consumer group ID ensures message load balancing and offsets management
            ConsumerConfig.GROUP_ID_CONFIG to "school-service",
            // Deserializer for message keys - Kafka stores keys as bytes, deserialize as String
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            // Deserializer for message values - we expect JSON representing StudentOnboardingEvent objects
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            // Allows deserialization of all packages (can be tightened to increase security)
            JsonDeserializer.TRUSTED_PACKAGES to "*"
        )
        // Create a DefaultKafkaConsumerFactory with config, specifying deserializers explicitly
        return DefaultKafkaConsumerFactory(
            config,
            StringDeserializer(),   // Key deserializer
            JsonDeserializer(StudentOnboardingEvent::class.java, false) // Value deserializer with target class
        )
    }
    /**
     * Defines a KafkaListenerContainerFactory bean that Spring Kafka uses to create listener containers.
     * Listener containers manage the consumer lifecycle and threading, allowing concurrent consumption.
     *
     * This factory uses the previously defined consumerFactory to create consumers .
     *
     * @return ConcurrentKafkaListenerContainerFactory<String, StudentOnboardingEvent> for annotated listeners
     */

    @Bean
    fun kafkaListenerFactory(): ConcurrentKafkaListenerContainerFactory<String, StudentOnboardingEvent> {
        // Instantiate the concurrent container factory
        val factory = ConcurrentKafkaListenerContainerFactory<String, StudentOnboardingEvent>()
        // Set the consumer factory to use in creating consumers for this listener container
        factory.consumerFactory = consumerFactory()
        // Return the configured factory for use by @KafkaListener annotations
        return factory
    }
}