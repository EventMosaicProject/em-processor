package com.neighbor.eventmosaic.processor.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neighbor.eventmosaic.library.common.dto.Event;
import com.neighbor.eventmosaic.library.common.dto.Mention;
import com.neighbor.eventmosaic.processor.component.BatchProcessor;
import com.neighbor.eventmosaic.processor.dto.BatchData;
import com.neighbor.eventmosaic.processor.exception.RedisOperationException;
import com.neighbor.eventmosaic.processor.exception.RedisSerializationException;
import com.neighbor.eventmosaic.processor.service.EventProcessingService;
import com.neighbor.eventmosaic.processor.util.RedisKeysUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Сервис для хранения и предварительной обработки событий и упоминаний.
 * Временно хранит данные в Redis до истечения временного окна батча.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessingServiceImpl implements EventProcessingService {

    private static final long EXTRA_TTL_MILLIS = 10_000;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final BatchProcessor batchProcessor;

    @Value("${batch.processing.window-duration-ms:60000}")
    private long batchWindowDurationMs;

    /**
     * Сохраняет событие в Redis для последующей обработки.
     * Устанавливает TTL для ключей, чтобы они автоматически удалялись.
     *
     * @param batchId идентификатор батча
     * @param event   событие для сохранения
     */
    @Override
    public void storeEvent(String batchId, Event event) {
        Long eventId = event.getGlobalEventId();
        String eventKey = RedisKeysUtil.buildEventKey(batchId, eventId);

        saveToRedis(eventKey, event);

        String setKey = RedisKeysUtil.buildBatchEventsSetKey(batchId);
        redisTemplate.opsForSet().add(setKey, eventId.toString());
        redisTemplate.expire(setKey, getEffectiveTtl());

        log.debug("Сохранено событие с ID {} для батча {} в Redis", eventId, batchId);
    }

    /**
     * Сохраняет упоминание в Redis для последующей обработки.
     * Устанавливает TTL для ключей, чтобы они автоматически удалялись.
     *
     * @param batchId идентификатор батча
     * @param mention упоминание для сохранения
     */
    @Override
    public void storeMention(String batchId, Mention mention) {
        String mentionId = mention.getGlobalEventId() + "_" + mention.getMentionIdentifier();
        String mentionKey = RedisKeysUtil.buildMentionKey(batchId, mentionId);

        saveToRedis(mentionKey, mention);

        String setKey = RedisKeysUtil.buildBatchMentionsSetKey(batchId);
        redisTemplate.opsForSet().add(setKey, mentionId);
        redisTemplate.expire(setKey, getEffectiveTtl());

        log.debug("Сохранено упоминание с ID {} для батча {} в Redis", mentionId, batchId);
    }

    /**
     * Обрабатывает данные конкретного батча.
     * Выполняет предварительную обработку событий и упоминаний.
     * В случае ошибки возвращает оригинальные данные.
     * Пока возвращает список событий и упоминаний без изменений.
     *
     * @param batchId идентификатор батча
     * @return объект BatchData
     */
    @Override
    public BatchData processBatch(String batchId) {
        log.info("Начало обработки данных батча {}", batchId);

        List<Event> events = getEventsForBatch(batchId);
        List<Mention> mentions = getMentionsForBatch(batchId);

        log.info("Батч {} содержит {} событий и {} упоминаний", batchId, events.size(), mentions.size());

        BatchData processed = batchProcessor.process(events, mentions);

        return new BatchData(processed.getEvents(), processed.getMentions());
    }

    /**
     * Возвращает все события для указанного батча из Redis.
     *
     * @param batchId идентификатор батча
     * @return список событий или пустой список
     */
    private List<Event> getEventsForBatch(String batchId) {
        return loadBatchData(
                RedisKeysUtil.buildBatchEventsSetKey(batchId),
                id -> RedisKeysUtil.buildEventKey(batchId, Long.valueOf(id)),
                Event.class
        );
    }

    /**
     * Возвращает все упоминания для указанного батча из Redis.
     *
     * @param batchId идентификатор батча
     * @return список упоминаний или пустой список
     */
    private List<Mention> getMentionsForBatch(String batchId) {
        return loadBatchData(
                RedisKeysUtil.buildBatchMentionsSetKey(batchId),
                id -> RedisKeysUtil.buildMentionKey(batchId, id),
                Mention.class
        );
    }


    /**
     * Сохраняет сериализованный объект в Redis с TTL.
     */
    private <T> void saveToRedis(String key, T object) {
        try {
            String json = objectMapper.writeValueAsString(object);
            redisTemplate.opsForValue().set(key, json);
            redisTemplate.expire(key, getEffectiveTtl());

        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации объекта {}: {}", object, e.getMessage(), e);
            throw new RedisSerializationException("Ошибка сериализации объекта для Redis", e);
        } catch (Exception e) {
            log.error("Ошибка сохранения в Redis для ключа {}: {}", key, e.getMessage(), e);
            throw new RedisOperationException("Ошибка сохранения объекта в Redis", e);
        }
    }

    /**
     * Загружает данные батча из Redis по ключам ID и преобразует в список объектов.
     *
     * @param setKey      ключ множества ID
     * @param keyResolver функция преобразования ID в ключ Redis
     * @param clazz       класс объекта для десериализации
     * @return список объектов
     */
    private <T> List<T> loadBatchData(String setKey,
                                      UnaryOperator<String> keyResolver,
                                      Class<T> clazz) {

        Set<String> ids = redisTemplate.opsForSet().members(setKey);
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        return ids.stream()
                .map(id -> {
                    String json = redisTemplate.opsForValue().get(keyResolver.apply(id));
                    if (json == null) {
                        return null;
                    }
                    try {
                        return objectMapper.readValue(json, clazz);
                    } catch (JsonProcessingException e) {
                        log.error("Ошибка десериализации объекта с ключом {}: {}", id, e.getMessage(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Возвращает итоговый TTL для ключей Redis.
     */
    private Duration getEffectiveTtl() {
        return Duration.ofMillis(batchWindowDurationMs + EXTRA_TTL_MILLIS);
    }
}
