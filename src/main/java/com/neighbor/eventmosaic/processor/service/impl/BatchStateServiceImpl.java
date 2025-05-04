package com.neighbor.eventmosaic.processor.service.impl;

import com.neighbor.eventmosaic.processor.service.BatchStateService;
import com.neighbor.eventmosaic.processor.util.RedisKeysUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Сервис для управления состоянием обработки батчей (пакетов) данных.
 * Использует Redis для отслеживания и обработки батчей в рамках временного окна.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchStateServiceImpl implements BatchStateService {

    private static final long EXTRA_TTL_MILLIS = 10_000;

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${batch.processing.window-duration-ms:60000}")
    private long batchWindowDurationMs;

    /**
     * Регистрирует новый батч или обновляет существующий.
     * Если это первый раз, когда встречается батч, начинает отсчет времени окна.
     *
     * @param batchId идентификатор батча
     * @return true если это новый батч, false если обновление существующего
     */
    @Override
    public boolean registerBatch(String batchId) {
        String startTimeKey = RedisKeysUtil.buildStartTimeKey(batchId);
        boolean isNewBatch = !redisTemplate.hasKey(startTimeKey);

        if (isNewBatch) {
            long currentTime = System.currentTimeMillis();
            redisTemplate.opsForValue().set(startTimeKey, String.valueOf(currentTime));
            redisTemplate.expire(startTimeKey, getEffectiveTtl());

            redisTemplate.opsForSet().add(RedisKeysUtil.activeBatchesSetKey(), batchId);

            log.info("Зарегистрирован новый батч: {}. Установлено временное окно: {} мс",
                    batchId, batchWindowDurationMs);
        }

        return isNewBatch;
    }

    /**
     * Проверяет батчи, время ожидания которых истекло, и помечает их как готовые к обработке.
     * Этот метод должен вызываться планировщиком.
     *
     * @return количество батчей, помеченных как готовые
     */
    @Override
    public int checkExpiredBatchWindows() {
        Set<String> activeBatches = redisTemplate.opsForSet().members(RedisKeysUtil.activeBatchesSetKey());
        if (activeBatches == null || activeBatches.isEmpty()) {
            return 0;
        }

        int processedCount = 0;
        long now = System.currentTimeMillis();

        for (String batchId : activeBatches) {
            String startTimeStr = redisTemplate.opsForValue().get(RedisKeysUtil.buildStartTimeKey(batchId));

            if (startTimeStr == null) {
                redisTemplate.opsForSet().remove(RedisKeysUtil.activeBatchesSetKey(), batchId);
                log.warn("Батч {} найден в активных, но без метки времени начала", batchId);
                continue;
            }

            long startTime = Long.parseLong(startTimeStr);
            if (now - startTime >= batchWindowDurationMs) {
                redisTemplate.opsForSet().add(RedisKeysUtil.readyBatchesSetKey(), batchId);     // Помечаем как готовый
                redisTemplate.opsForSet().remove(RedisKeysUtil.activeBatchesSetKey(), batchId); // Удаляем из активных

                log.info("Батч {} готов к обработке после истечения времени окна ({} мс)", batchId, now - startTime);
                processedCount++;
            }
        }
        return processedCount;
    }

    /**
     * Получает и удаляет один готовый для обработки батч из списка.
     *
     * @return идентификатор батча или null, если нет готовых батчей
     */
    @Override
    public String getNextReadyBatch() {
        return redisTemplate.opsForSet()
                .pop(RedisKeysUtil.readyBatchesSetKey());
    }


    /**
     * Возвращает итоговый TTL для ключей Redis.
     * Включает время окна обработки и дополнительное время.
     */
    private Duration getEffectiveTtl() {
        return Duration.ofMillis(batchWindowDurationMs + EXTRA_TTL_MILLIS);
    }
}
