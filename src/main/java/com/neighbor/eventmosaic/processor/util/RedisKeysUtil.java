package com.neighbor.eventmosaic.processor.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RedisKeysUtil {

    // Префиксы для данных
    private static final String EVENT_DATA_PREFIX = "data:event:";
    private static final String MENTION_DATA_PREFIX = "data:mention:";
    private static final String BATCH_EVENTS_KEY_PREFIX = "batch:events:"; // Множество ID событий
    private static final String BATCH_MENTIONS_KEY_PREFIX = "batch:mentions:"; // Множество ID упоминаний

    // Префиксы/ключи для состояния
    private static final String BATCH_START_TIME_KEY_PREFIX = "batch:start:";
    private static final String ACTIVE_BATCHES_KEY = "active:batches"; // Множество активных батчей
    private static final String READY_BATCHES_KEY = "ready:batches";   // Множество готовых батчей

    /* Данные */
    public static String buildEventKey(String batchId, Long eventId) {
        return EVENT_DATA_PREFIX + batchId + ":" + eventId;
    }

    public static String buildMentionKey(String batchId, String mentionId) {
        return MENTION_DATA_PREFIX + batchId + ":" + mentionId;
    }

    public static String buildBatchEventsSetKey(String batchId) {
        return BATCH_EVENTS_KEY_PREFIX + batchId;
    }

    public static String buildBatchMentionsSetKey(String batchId) {
        return BATCH_MENTIONS_KEY_PREFIX + batchId;
    }

    /* Состояние */
    public static String buildStartTimeKey(String batchId) {
        return BATCH_START_TIME_KEY_PREFIX + batchId;
    }

    public static String activeBatchesSetKey() {
        return ACTIVE_BATCHES_KEY;
    }

    public static String readyBatchesSetKey() {
        return READY_BATCHES_KEY;
    }
}
