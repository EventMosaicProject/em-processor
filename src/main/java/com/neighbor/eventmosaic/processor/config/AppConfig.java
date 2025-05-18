package com.neighbor.eventmosaic.processor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

/**
 * Общий конфигурационный класс для приложения
 */
@Configuration
public class AppConfig {

    /**
     * Создает и настраивает KafkaTemplate для отправки сообщений в Kafka.
     * Использует JsonSerializer для сериализации объектов в JSON, в частности для понятного отображения даты в топике.
     *
     * @param kafkaProperties свойства Kafka, автоматически настроенные Spring Boot
     * @param objectMapper    ObjectMapper для сериализации объектов в JSON
     * @return настроенный KafkaTemplate
     */
    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate(KafkaProperties kafkaProperties,
                                                       ObjectMapper objectMapper) {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        ProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new JsonSerializer<>(objectMapper)
        );
        return new KafkaTemplate<>(factory);
    }
}
