package com.neighbor.eventmosaic.processor.component;

import com.neighbor.eventmosaic.processor.util.RedisKeysUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Класс для очистки Redis для указанного батча.
 * Выполняет полную очистку данных и состояния для указанного батча в Redis.
 * Удаляет данные по событиям, упоминаниям и состоянию.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisBatchCleaner {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Выполняет полную очистку данных и состояния для указанного батча в Redis.
     *
     * @param batchId идентификатор батча
     */
    public void cleanupBatch(String batchId) {
        log.info("Запуск полной очистки Redis для батча {}", batchId);
        try {
            // Удаление данных по событиям
            cleanupDataSet(RedisKeysUtil.buildBatchEventsSetKey(batchId),
                    id -> RedisKeysUtil.buildEventKey(batchId, Long.valueOf(id)));

            // Удаление данных по упоминаниям
            cleanupDataSet(RedisKeysUtil.buildBatchMentionsSetKey(batchId),
                    id -> RedisKeysUtil.buildMentionKey(batchId, id));

            // Удаление состояния
            redisTemplate.opsForSet().remove(RedisKeysUtil.readyBatchesSetKey(), batchId);  // Из готовых
            redisTemplate.opsForSet().remove(RedisKeysUtil.activeBatchesSetKey(), batchId); // Из активных (на всякий случай)
            redisTemplate.delete(RedisKeysUtil.buildStartTimeKey(batchId));                 // Время старта

            log.info("Полная очистка Redis для батча {} успешно завершена", batchId);

        } catch (Exception e) {
            log.error("Ошибка во время полной очистки Redis для батча {}: {}", batchId, e.getMessage(), e);
        }
    }

    /**
     * Метод для удаления множества ключей данных.
     * Сначала получает все ID из множества, затем удаляет ключи данных и само множество.
     */
    private void cleanupDataSet(String setKey,
                                UnaryOperator<String> keyResolver) {

        Set<String> ids = redisTemplate.opsForSet().members(setKey);

        if (ids != null && !ids.isEmpty()) {
            List<String> dataKeys = ids.stream()
                    .map(keyResolver)
                    .toList();

            Long deletedCount = redisTemplate.delete(dataKeys);
            log.debug("Удалено {} ключей данных для множества {}", deletedCount, setKey);
        }
        redisTemplate.delete(setKey);
        log.debug("Удалено множество ключей {}", setKey);
    }
}
