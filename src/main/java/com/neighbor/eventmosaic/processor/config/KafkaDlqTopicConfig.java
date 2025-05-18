package com.neighbor.eventmosaic.processor.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Конфигурация для создания топиков Dead Letter Queue (DLQ) в Kafka.
 * Эти топики используются для хранения сообщений, которые не удалось обработать.
 */
@Configuration
public class KafkaDlqTopicConfig {

    @Value("${kafka.topic.dlq-connect.event}")
    private String eventConnectDlqTopicName;

    @Value("${kafka.topic.dlq-connect.mention}")
    private String mentionConnectDlqTopicName;

    @Bean
    public NewTopic eventConnectDlqTopic() {
        return TopicBuilder.name(eventConnectDlqTopicName)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic mentionConnectDlqTopic() {
        return TopicBuilder.name(mentionConnectDlqTopicName)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
