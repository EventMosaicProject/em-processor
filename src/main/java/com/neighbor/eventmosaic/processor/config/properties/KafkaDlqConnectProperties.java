package com.neighbor.eventmosaic.processor.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурационные свойства для DLQ топиков Kafka Connect.
 * Связывается с префиксом "kafka.topic.dlq-connect" в application.yml.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "kafka.topic.dlq-connect")
public class KafkaDlqConnectProperties {

    private TopicConfig event;
    private TopicConfig mention;

    /**
     * Конфигурация отдельного DLQ топика.
     */
    @Getter
    @Setter
    public static class TopicConfig {
        private String name;
        private int partitions;
        private int replicas;
    }
} 