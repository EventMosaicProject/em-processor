package com.neighbor.eventmosaic.processor.config;

import com.neighbor.eventmosaic.processor.config.properties.KafkaDlqConnectProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Конфигурация для создания топиков Dead Letter Queue (DLQ) в Kafka.
 * Эти топики используются для хранения сообщений, которые не удалось обработать.
 */
@Configuration
@RequiredArgsConstructor
public class KafkaDlqTopicConfig {

    private final KafkaDlqConnectProperties dlqConnectProperties;

    @Bean
    public NewTopic eventConnectDlqTopic() {
        KafkaDlqConnectProperties.TopicConfig eventConfig = dlqConnectProperties.getEvent();

        return TopicBuilder.name(eventConfig.getName())
                .partitions(eventConfig.getPartitions())
                .replicas(eventConfig.getReplicas())
                .build();
    }

    @Bean
    public NewTopic mentionConnectDlqTopic() {
        KafkaDlqConnectProperties.TopicConfig mentionConfig = dlqConnectProperties.getMention();

        return TopicBuilder.name(mentionConfig.getName())
                .partitions(mentionConfig.getPartitions())
                .replicas(mentionConfig.getReplicas())
                .build();
    }
}
