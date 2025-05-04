package com.neighbor.eventmosaic.processor.publisher;

import com.neighbor.eventmosaic.library.common.dto.Event;
import com.neighbor.eventmosaic.library.common.dto.Mention;
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
    public CompletableFuture<SendResult<String, Object>> sendEvent(Event event, String key) {
        log.debug("Отправка события с ID {} в топик {}", event.getGlobalEventId(), eventTopic);

        return kafkaTemplate.send(eventTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Событие с ID {} успешно отправлено, смещение: {}",
                                event.getGlobalEventId(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Ошибка при отправке события с ID {}: {}",
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
    public CompletableFuture<SendResult<String, Object>> sendMention(Mention mention, String key) {
        log.debug("Отправка упоминания для события с ID {} в топик {}",
                mention.getGlobalEventId(), mentionTopic);

        return kafkaTemplate.send(mentionTopic, key, mention)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Упоминание для события с ID {} успешно отправлено, смещение: {}",
                                mention.getGlobalEventId(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Ошибка при отправке упоминания для события с ID {}: {}",
                                mention.getGlobalEventId(),
                                ex.getMessage(), ex);
                    }
                });
    }
}