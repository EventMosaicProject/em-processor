package com.neighbor.eventmosaic.processor.component;

import com.neighbor.eventmosaic.processor.testcontainer.RedisTestContainerInitializer;
import com.neighbor.eventmosaic.processor.util.RedisKeysUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class RedisBatchCleanerIntegrationTest implements RedisTestContainerInitializer {

    @Autowired
    private RedisBatchCleaner redisBatchCleaner;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String TEST_BATCH_ID = "20250323151500";
    private static final Long TEST_EVENT_ID = 123456L;
    private static final String TEST_MENTION_ID = "789012";

    @BeforeEach
    void setUp() {
        // Очищаем Redis перед каждым тестом
        Objects.requireNonNull(redisTemplate.getConnectionFactory())
                .getConnection()
                .serverCommands()
                .flushDb();
    }

    @Test
    @DisplayName("Должен успешно очистить все данные батча из Redis")
    void shouldSuccessfullyCleanupBatchData() {
        // Arrange
        String eventKey = RedisKeysUtil.buildEventKey(TEST_BATCH_ID, TEST_EVENT_ID);
        String mentionKey = RedisKeysUtil.buildMentionKey(TEST_BATCH_ID, TEST_MENTION_ID);
        String eventsSetKey = RedisKeysUtil.buildBatchEventsSetKey(TEST_BATCH_ID);
        String mentionsSetKey = RedisKeysUtil.buildBatchMentionsSetKey(TEST_BATCH_ID);
        String startTimeKey = RedisKeysUtil.buildStartTimeKey(TEST_BATCH_ID);

        // Добавляем события
        redisTemplate.opsForValue().set(eventKey, "test_event_data");
        redisTemplate.opsForSet().add(eventsSetKey, TEST_EVENT_ID.toString());

        // Добавляем упоминания
        redisTemplate.opsForValue().set(mentionKey, "test_mention_data");
        redisTemplate.opsForSet().add(mentionsSetKey, TEST_MENTION_ID);

        // Добавляем батч в активные и готовые
        redisTemplate.opsForSet().add(RedisKeysUtil.activeBatchesSetKey(), TEST_BATCH_ID);
        redisTemplate.opsForSet().add(RedisKeysUtil.readyBatchesSetKey(), TEST_BATCH_ID);
        redisTemplate.opsForValue().set(startTimeKey, String.valueOf(System.currentTimeMillis()));

        // Проверяем, что данные действительно добавлены
        assertThat(redisTemplate.hasKey(eventKey)).isTrue();
        assertThat(redisTemplate.hasKey(mentionKey)).isTrue();
        assertThat(redisTemplate.hasKey(eventsSetKey)).isTrue();
        assertThat(redisTemplate.hasKey(mentionsSetKey)).isTrue();
        assertThat(redisTemplate.hasKey(startTimeKey)).isTrue();

        // Act
        redisBatchCleaner.cleanupBatch(TEST_BATCH_ID);

        // Assert
        assertThat(redisTemplate.hasKey(eventKey)).isFalse();
        assertThat(redisTemplate.hasKey(mentionKey)).isFalse();
        assertThat(redisTemplate.hasKey(eventsSetKey)).isFalse();
        assertThat(redisTemplate.hasKey(mentionsSetKey)).isFalse();
        assertThat(redisTemplate.hasKey(startTimeKey)).isFalse();

        // Проверяем, что батч удален из активных и готовых наборов
        Set<String> activeBatches = redisTemplate.opsForSet().members(RedisKeysUtil.activeBatchesSetKey());
        Set<String> readyBatches = redisTemplate.opsForSet().members(RedisKeysUtil.readyBatchesSetKey());

        assertThat(activeBatches).doesNotContain(TEST_BATCH_ID).isEmpty();
        assertThat(readyBatches).doesNotContain(TEST_BATCH_ID).isEmpty();
    }

    @Test
    @DisplayName("Должен корректно обрабатывать пустые наборы данных")
    void shouldHandleEmptyDataSets() {
        // Arrange
        // Создаем пустые сеты
        redisTemplate.opsForSet().add(RedisKeysUtil.activeBatchesSetKey(), TEST_BATCH_ID);
        redisTemplate.opsForSet().add(RedisKeysUtil.readyBatchesSetKey(), TEST_BATCH_ID);

        // Проверяем наличие ключей перед очисткой
        assertThat(redisTemplate.hasKey(RedisKeysUtil.activeBatchesSetKey())).isTrue();
        assertThat(redisTemplate.hasKey(RedisKeysUtil.readyBatchesSetKey())).isTrue();

        // Act
        redisBatchCleaner.cleanupBatch(TEST_BATCH_ID);

        // Assert
        Set<String> activeBatches = redisTemplate.opsForSet().members(RedisKeysUtil.activeBatchesSetKey());
        Set<String> readyBatches = redisTemplate.opsForSet().members(RedisKeysUtil.readyBatchesSetKey());

        assertThat(activeBatches).doesNotContain(TEST_BATCH_ID).isEmpty();
        assertThat(readyBatches).doesNotContain(TEST_BATCH_ID).isEmpty();
    }

    @Test
    @DisplayName("Должен корректно обрабатывать несуществующие батчи")
    void shouldHandleNonExistentBatch() {
        // Arrange:
        String nonExistentBatchId = "nonexistent123";

        // Act
        redisBatchCleaner.cleanupBatch(nonExistentBatchId);

        // Assert
        assertThat(redisTemplate.keys("*")).isEmpty();
    }
}