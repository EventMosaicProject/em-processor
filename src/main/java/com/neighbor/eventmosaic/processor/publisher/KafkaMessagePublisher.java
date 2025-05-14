package com.neighbor.eventmosaic.processor.publisher;

import com.neighbor.eventmosaic.library.common.dto.Event;
import com.neighbor.eventmosaic.library.common.dto.Mention;
import com.neighbor.eventmosaic.processor.dto.ElasticEvent;
import com.neighbor.eventmosaic.processor.dto.ElasticMention;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Компонент для отправки обработанных событий и упоминаний в топики Kafka.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMessagePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.producer.processor-event}")
    private String eventTopic;

    @Value("${kafka.topic.producer.processor-mention}")
    private String mentionTopic;

    /**
     * Отправляет обработанное событие в соответствующий топик Kafka.
     * После отправки проверяется, была ли отправка успешной.
     *
     * @param event объект события для отправки
     * @param key   ключ сообщения (идентификатор события)
     * @return CompletableFuture с результатом отправки
     */
    public CompletableFuture<SendResult<String, Object>> sendEvent(ElasticEvent event,
                                                                   String key) {
        log.debug("Отправка ElasticEvent с ID {} в топик {}", event.getGlobalEventId(), eventTopic);

        return kafkaTemplate.send(eventTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("ElasticEvent с ID {} успешно отправлен, смещение: {}",
                                event.getGlobalEventId(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Ошибка при отправке ElasticEvent с ID {}: {}",
                                event.getGlobalEventId(),
                                ex.getMessage(), ex);
                    }
                });
    }

    /**
     * Отправляет обработанное упоминание в соответствующий топик Kafka.
     * После отправки проверяется, была ли отправка успешной.
     *
     * @param mention объект упоминания для отправки
     * @param key     ключ сообщения (комбинация идентификаторов события и упоминания)
     * @return CompletableFuture с результатом отправки
     */
    public CompletableFuture<SendResult<String, Object>> sendMention(ElasticMention mention,
                                                                     String key) {
        log.debug("Отправка ElasticMention для события с ID {} (Упоминание ID: {}) в топик {}",
                mention.getGlobalEventId(), mention.getMentionIdentifier(), mentionTopic);

        return kafkaTemplate.send(mentionTopic, key, mention)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("ElasticMention для события с ID {} (Упоминание ID: {}) успешно отправлено, смещение: {}",
                                mention.getGlobalEventId(), mention.getMentionIdentifier(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Ошибка при отправке ElasticMention для события с ID {} (Упоминание ID: {}): {}",
                                mention.getGlobalEventId(), mention.getMentionIdentifier(),
                                ex.getMessage(), ex);
                    }
                });
    }
}