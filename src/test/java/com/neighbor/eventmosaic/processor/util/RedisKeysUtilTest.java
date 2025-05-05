package com.neighbor.eventmosaic.processor.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedisKeysUtilTest {

    private static final String TEST_BATCH_ID = "20250323151500";
    private static final Long TEST_EVENT_ID = 123456L;
    private static final String TEST_MENTION_ID = "789012";

    @Test
    @DisplayName("buildEventKey должен формировать корректный ключ для события")
    void buildEventKey_shouldCreateCorrectEventKey() {
        // Act
        String result = RedisKeysUtil.buildEventKey(TEST_BATCH_ID, TEST_EVENT_ID);

        // Assert
        assertEquals("data:event:" + TEST_BATCH_ID + ":" + TEST_EVENT_ID, result);
    }

    @Test
    @DisplayName("buildMentionKey должен формировать корректный ключ для упоминания")
    void buildMentionKey_shouldCreateCorrectMentionKey() {
        // Act
        String result = RedisKeysUtil.buildMentionKey(TEST_BATCH_ID, TEST_MENTION_ID);

        // Assert
        assertEquals("data:mention:" + TEST_BATCH_ID + ":" + TEST_MENTION_ID, result);
    }

    @Test
    @DisplayName("buildBatchEventsSetKey должен формировать корректный ключ для множества событий")
    void buildBatchEventsSetKey_shouldCreateCorrectEventsSetKey() {
        // Act
        String result = RedisKeysUtil.buildBatchEventsSetKey(TEST_BATCH_ID);

        // Assert
        assertEquals("batch:events:" + TEST_BATCH_ID, result);
    }

    @Test
    @DisplayName("buildBatchMentionsSetKey должен формировать корректный ключ для множества упоминаний")
    void buildBatchMentionsSetKey_shouldCreateCorrectMentionsSetKey() {
        // Act
        String result = RedisKeysUtil.buildBatchMentionsSetKey(TEST_BATCH_ID);

        // Assert
        assertEquals("batch:mentions:" + TEST_BATCH_ID, result);
    }

    @Test
    @DisplayName("buildStartTimeKey должен формировать корректный ключ для времени старта батча")
    void buildStartTimeKey_shouldCreateCorrectStartTimeKey() {
        // Act
        String result = RedisKeysUtil.buildStartTimeKey(TEST_BATCH_ID);

        // Assert
        assertEquals("batch:start:" + TEST_BATCH_ID, result);
    }

    @Test
    @DisplayName("activeBatchesSetKey должен возвращать корректный ключ для множества активных батчей")
    void activeBatchesSetKey_shouldReturnCorrectActiveBatchesKey() {
        // Act
        String result = RedisKeysUtil.activeBatchesSetKey();

        // Assert
        assertEquals("active:batches", result);
    }

    @Test
    @DisplayName("readyBatchesSetKey должен возвращать корректный ключ для множества готовых батчей")
    void readyBatchesSetKey_shouldReturnCorrectReadyBatchesKey() {
        // Act
        String result = RedisKeysUtil.readyBatchesSetKey();

        // Assert
        assertEquals("ready:batches", result);
    }
}