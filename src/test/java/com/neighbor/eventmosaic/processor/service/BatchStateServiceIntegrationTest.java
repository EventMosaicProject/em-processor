package com.neighbor.eventmosaic.processor.service;

import com.neighbor.eventmosaic.processor.service.impl.BatchStateServiceImpl;
import com.neighbor.eventmosaic.processor.testcontainer.RedisTestContainerInitializer;
import com.neighbor.eventmosaic.processor.util.RedisKeysUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class BatchStateServiceIntegrationTest implements RedisTestContainerInitializer {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @MockitoSpyBean
    private BatchStateServiceImpl batchStateService;

    private static final String TEST_BATCH_ID = "20250323151500";
    private static final String TEST_BATCH_ID_2 = "20250323151600";
    private static final long WINDOW_DURATION_MS = 60000; // 60 секунд

    @BeforeEach
    void setUp() {
        // Очищаем Redis перед каждым тестом
        Objects.requireNonNull(redisTemplate.getConnectionFactory())
                .getConnection()
                .serverCommands()
                .flushDb();
    }

    @Test
    @DisplayName("Должен зарегистрировать новый батч с временем старта и добавить в активные")
    void registerBatch_shouldRegisterNewBatchWithStartTime() {
        // Act
        boolean isNew = batchStateService.registerBatch(TEST_BATCH_ID);

        // Assert
        assertThat(isNew).isTrue();
        assertThat(redisTemplate.hasKey(RedisKeysUtil.buildStartTimeKey(TEST_BATCH_ID))).isTrue();
        assertThat(redisTemplate.opsForSet().members(RedisKeysUtil.activeBatchesSetKey()))
                .contains(TEST_BATCH_ID);
    }

    @Test
    @DisplayName("Должен вернуть false при повторной регистрации существующего батча")
    void registerBatch_shouldReturnFalseForExistingBatch() {
        // Arrange
        batchStateService.registerBatch(TEST_BATCH_ID);

        // Act
        boolean isNew = batchStateService.registerBatch(TEST_BATCH_ID);

        // Assert
        assertThat(isNew).isFalse();
        assertThat(redisTemplate.opsForSet().members(RedisKeysUtil.activeBatchesSetKey()))
                .containsOnlyOnce(TEST_BATCH_ID);
    }

    @Test
    @DisplayName("checkExpiredBatchWindows должен помечать батчи с истекшим временем как готовые")
    void checkExpiredBatchWindows_shouldMarkExpiredBatchesAsReady() {
        // Arrange
        long expiredTime = System.currentTimeMillis() - WINDOW_DURATION_MS - 1000; // время старта + окно + 1 секунда

        // Регистрируем батч и устанавливаем время старта на "просроченное"
        redisTemplate.opsForSet().add(RedisKeysUtil.activeBatchesSetKey(), TEST_BATCH_ID);
        redisTemplate.opsForValue().set(RedisKeysUtil.buildStartTimeKey(TEST_BATCH_ID),
                String.valueOf(expiredTime));

        // Act
        int markedCount = batchStateService.checkExpiredBatchWindows();

        // Assert
        assertThat(markedCount).isEqualTo(1);
        assertThat(redisTemplate.opsForSet().members(RedisKeysUtil.readyBatchesSetKey()))
                .contains(TEST_BATCH_ID);
        assertThat(redisTemplate.opsForSet().members(RedisKeysUtil.activeBatchesSetKey()))
                .doesNotContain(TEST_BATCH_ID);
    }

    @Test
    @DisplayName("checkExpiredBatchWindows не должен помечать батчи, если время не истекло")
    void checkExpiredBatchWindows_shouldNotMarkNonExpiredBatches() {
        // Arrange
        long recentTime = System.currentTimeMillis() - 1000; // 1 секунда назад

        redisTemplate.opsForSet().add(RedisKeysUtil.activeBatchesSetKey(), TEST_BATCH_ID);
        redisTemplate.opsForValue().set(RedisKeysUtil.buildStartTimeKey(TEST_BATCH_ID),
                String.valueOf(recentTime));

        // Act
        int markedCount = batchStateService.checkExpiredBatchWindows();

        // Assert
        assertThat(markedCount).isZero();
        assertThat(redisTemplate.opsForSet().members(RedisKeysUtil.readyBatchesSetKey()))
                .doesNotContain(TEST_BATCH_ID);
        assertThat(redisTemplate.opsForSet().members(RedisKeysUtil.activeBatchesSetKey()))
                .contains(TEST_BATCH_ID);
    }

    @Test
    @DisplayName("checkExpiredBatchWindows должен обрабатывать несколько батчей корректно")
    void checkExpiredBatchWindows_shouldProcessMultipleBatchesCorrectly() {
        // Arrange
        long expiredTime = System.currentTimeMillis() - WINDOW_DURATION_MS - 1000;
        long recentTime = System.currentTimeMillis() - 1000;

        // Добавляем просроченный батч
        redisTemplate.opsForSet().add(RedisKeysUtil.activeBatchesSetKey(), TEST_BATCH_ID);
        redisTemplate.opsForValue().set(RedisKeysUtil.buildStartTimeKey(TEST_BATCH_ID),
                String.valueOf(expiredTime));

        // Добавляем свежий батч
        redisTemplate.opsForSet().add(RedisKeysUtil.activeBatchesSetKey(), TEST_BATCH_ID_2);
        redisTemplate.opsForValue().set(RedisKeysUtil.buildStartTimeKey(TEST_BATCH_ID_2),
                String.valueOf(recentTime));

        // Act
        int markedCount = batchStateService.checkExpiredBatchWindows();

        // Assert
        assertThat(markedCount).isEqualTo(1);
        assertThat(redisTemplate.opsForSet().members(RedisKeysUtil.readyBatchesSetKey()))
                .contains(TEST_BATCH_ID)
                .doesNotContain(TEST_BATCH_ID_2);
        assertThat(redisTemplate.opsForSet().members(RedisKeysUtil.activeBatchesSetKey()))
                .contains(TEST_BATCH_ID_2)
                .doesNotContain(TEST_BATCH_ID);
    }

    @Test
    @DisplayName("checkExpiredBatchWindows должен удалять батчи без временной метки из активных")
    void checkExpiredBatchWindows_shouldRemoveBatchesWithoutTimestamp() {
        // Arrange
        redisTemplate.opsForSet().add(RedisKeysUtil.activeBatchesSetKey(), TEST_BATCH_ID);
        // не добавляем временную метку

        // Act
        int markedCount = batchStateService.checkExpiredBatchWindows();

        // Assert
        assertThat(markedCount).isZero();
        assertThat(redisTemplate.opsForSet().members(RedisKeysUtil.activeBatchesSetKey()))
                .doesNotContain(TEST_BATCH_ID);
        assertThat(redisTemplate.opsForSet().members(RedisKeysUtil.readyBatchesSetKey()))
                .doesNotContain(TEST_BATCH_ID);
    }

    @Test
    @DisplayName("checkExpiredBatchWindows должен возвращать 0 при пустом наборе активных батчей")
    void checkExpiredBatchWindows_shouldReturnZeroForEmptyActiveBatches() {
        // ничего не добавляем в Redis

        // Act
        int markedCount = batchStateService.checkExpiredBatchWindows();

        // Assert
        assertThat(markedCount).isZero();
    }

    @Test
    @DisplayName("getNextReadyBatch должен возвращать и удалять батч из набора готовых")
    void getNextReadyBatch_shouldReturnAndRemoveBatchFromReadySet() {
        // Arrange
        redisTemplate.opsForSet().add(RedisKeysUtil.readyBatchesSetKey(), TEST_BATCH_ID);

        // Act
        String nextBatch = batchStateService.getNextReadyBatch();

        // Assert
        assertThat(nextBatch).isEqualTo(TEST_BATCH_ID);
        assertThat(redisTemplate.opsForSet().members(RedisKeysUtil.readyBatchesSetKey()))
                .doesNotContain(TEST_BATCH_ID)
                .isEmpty();
    }

    @Test
    @DisplayName("getNextReadyBatch должен возвращать null, если нет готовых батчей")
    void getNextReadyBatch_shouldReturnNullWhenNoReadyBatches() {
        // не добавляем готовых батчей

        // Act
        String nextBatch = batchStateService.getNextReadyBatch();

        // Assert
        assertThat(nextBatch).isNull();
    }

    @Test
    @DisplayName("getNextReadyBatch должен обрабатывать несколько батчей в правильном порядке")
    void getNextReadyBatch_shouldHandleMultipleBatches() {
        // Arrange
        redisTemplate.opsForSet().add(RedisKeysUtil.readyBatchesSetKey(), TEST_BATCH_ID);
        redisTemplate.opsForSet().add(RedisKeysUtil.readyBatchesSetKey(), TEST_BATCH_ID_2);

        // Act & Assert
        // Так как Redis Set не гарантирует порядок, проверим что оба батча обрабатываются и после этого набор пустой
        String batch1 = batchStateService.getNextReadyBatch();
        String batch2 = batchStateService.getNextReadyBatch();
        String batch3 = batchStateService.getNextReadyBatch();

        assertThat(Set.of(batch1, batch2)).containsExactlyInAnyOrder(TEST_BATCH_ID, TEST_BATCH_ID_2);
        assertThat(batch3).isNull();
        assertThat(redisTemplate.opsForSet().members(RedisKeysUtil.readyBatchesSetKey()))
                .isEmpty();
    }
}