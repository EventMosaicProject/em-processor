package com.neighbor.eventmosaic.processor.scheduler;

import com.neighbor.eventmosaic.processor.dto.BatchData;
import com.neighbor.eventmosaic.processor.service.BatchStateService;
import com.neighbor.eventmosaic.processor.service.EventProcessingService;
import com.neighbor.eventmosaic.processor.publisher.KafkaMessagePublisher;
import com.neighbor.eventmosaic.processor.component.RedisBatchCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Планировщик для периодической проверки и обработки батчей.
 * <p>
 * Основные задачи:
 * 1. Проверяет, истекло ли окно ожидания у активных батчей.
 * 2. Если батч готов — извлекает его, обрабатывает события и упоминания.
 * 3. Отправляет данные в Kafka и после успешной отправки очищает состояние.
 * <p>
 * Сейчас считаем, что в каждый момент времени активен только один батч.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchProcessingScheduler {

    private final BatchStateService batchStateService;
    private final EventProcessingService eventProcessingService;
    private final KafkaMessagePublisher kafkaMessagePublisher;
    private final RedisBatchCleaner redisBatchCleaner;

    /**
     * Проверяет, истекло ли время окна ожидания у активных батчей.
     * Если да — помечает такие батчи как готовые к обработке.
     * Запускается каждые 5 секунд.
     */
    @Scheduled(fixedDelayString = "5000")
    public void checkBatchWindows() {
        int expiredBatchCount = batchStateService.checkExpiredBatchWindows();
        if (expiredBatchCount > 0) {
            log.info("Обнаружено батчей с истекшим временем окна - {}", expiredBatchCount);
        }
    }

    /**
     * Обрабатывает один готовый к отправке батч.
     * Получает данные, отправляет их в Kafka, очищает состояние после успешной отправки.
     * Запускается каждые 3 секунды.
     */
    @Scheduled(fixedDelayString = "3000")
    public void processBatchIfReady() {
        String batchId = batchStateService.getNextReadyBatch();

        if (batchId == null) {
            log.debug("Нет готовых батчей для обработки");
            return; // Нет батчей для обработки
        }

        try {
            log.info("Начало обработки батча: {}", batchId);

            BatchData batchData = eventProcessingService.processBatch(batchId);

            CompletableFuture<Void> sendEventsFuture = sendEvents(batchData);
            CompletableFuture<Void> sendMentionsFuture = sendMentions(batchData);

            // Ждём завершения всех отправок и обрабатываем результат
            CompletableFuture.allOf(sendEventsFuture, sendMentionsFuture)
                    .whenComplete((ignored, ex) -> {
                        if (ex == null) {
                            log.info("Батч {} успешно обработан и отправлен", batchId);
                            redisBatchCleaner.cleanupBatch(batchId);
                        } else {
                            log.error("Ошибка при отправке данных для батча {}: {}", batchId, ex.getMessage(), ex);
                        }
                    });

        } catch (Exception e) {
            log.error("Ошибка при обработке батча {}: {}", batchId, e.getMessage(), e);
            // cleanupBatch не вызываем, если была ошибка обработки

        }
    }

    /**
     * Отправляет все события батча в Kafka.
     * Вид ключа: globalEventId
     *
     * @param batchData объект с событиями и упоминаниями
     * @return CompletableFuture, который завершится после отправки всех событий
     */
    private CompletableFuture<Void> sendEvents(BatchData batchData) {

        return CompletableFuture.allOf(batchData.getEvents().stream()
                .map(elasticEvent -> kafkaMessagePublisher.sendEvent(
                        elasticEvent,
                        String.valueOf(elasticEvent.getGlobalEventId())))
                .toArray(CompletableFuture[]::new));
    }

    /**
     * Отправляет все упоминания батча в Kafka.
     * Вид ключа: globalEventId_mentionIdentifier
     *
     * @param batchData объект с событиями и упоминаниями
     * @return CompletableFuture, который завершится после отправки всех упоминаний
     */
    private CompletableFuture<Void> sendMentions(BatchData batchData) {

        return CompletableFuture.allOf(batchData.getMentions().stream()
                .map(elasticMention -> kafkaMessagePublisher.sendMention(
                        elasticMention,
                        elasticMention.getGlobalEventId() + "_" + elasticMention.getMentionIdentifier()))
                .toArray(CompletableFuture[]::new));
    }
}
