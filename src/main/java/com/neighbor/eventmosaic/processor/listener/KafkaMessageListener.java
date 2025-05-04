package com.neighbor.eventmosaic.processor.listener;

import com.neighbor.eventmosaic.library.common.dto.Event;
import com.neighbor.eventmosaic.library.common.dto.Mention;
import com.neighbor.eventmosaic.processor.service.BatchStateService;
import com.neighbor.eventmosaic.processor.service.EventProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Сервис для приема и обработки сообщений из Kafka.
 * Сохраняет события и упоминания в Redis в рамках временного окна.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMessageListener {

    private static final String BATCH_HEADER = "X-Batch-ID";

    private final BatchStateService batchStateService;
    private final EventProcessingService eventProcessingService;

    /**
     * Обрабатывает сообщения с событиями из входного топика.
     * Сохраняет полученное событие и регистрирует батч если нужно.
     *
     * @param event   событие из сообщения
     * @param batchId идентификатор батча из заголовка
     */
    @KafkaListener(
            topics = "${kafka.topic.consumer.adapter-event}")
    public void consumeEvent(@Payload Event event,
                             @Header(value = BATCH_HEADER) String batchId) {

        handleBatchRegistration(batchId);
        log.debug("Получено событие с ID {} из батча {}", event.getGlobalEventId(), batchId);
        eventProcessingService.storeEvent(batchId, event);
    }

    /**
     * Обрабатывает сообщения с упоминаниями из входного топика.
     * Сохраняет полученное упоминание и регистрирует батч если нужно.
     *
     * @param mention упоминание из сообщения
     * @param batchId идентификатор батча из заголовка
     */
    @KafkaListener(
            topics = "${kafka.topic.consumer.adapter-mention}")
    public void consumeMention(@Payload Mention mention,
                               @Header(value = BATCH_HEADER) String batchId) {

        handleBatchRegistration(batchId);
        log.debug("Получено упоминание для события с ID {} из батча {}",
                mention.getGlobalEventId(), batchId);
        eventProcessingService.storeMention(batchId, mention);
    }

    /**
     * Общая логика регистрации батча и логгирования начала временного окна.
     *
     * @param batchId идентификатор батча
     */
    private void handleBatchRegistration(String batchId) {
        boolean isNewBatch = batchStateService.registerBatch(batchId);
        if (isNewBatch) {
            log.info("Начато временное окно для нового батча: {}", batchId);
        }
    }
}
