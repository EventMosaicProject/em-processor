package com.neighbor.eventmosaic.processor.scheduler;

import com.neighbor.eventmosaic.library.common.dto.Event;
import com.neighbor.eventmosaic.library.common.dto.Mention;
import com.neighbor.eventmosaic.processor.component.RedisBatchCleaner;
import com.neighbor.eventmosaic.processor.dto.BatchData;
import com.neighbor.eventmosaic.processor.dto.ElasticEvent;
import com.neighbor.eventmosaic.processor.dto.ElasticMention;
import com.neighbor.eventmosaic.processor.publisher.KafkaMessagePublisher;
import com.neighbor.eventmosaic.processor.service.BatchStateService;
import com.neighbor.eventmosaic.processor.service.EventProcessingService;
import com.neighbor.eventmosaic.processor.testcontainer.KafkaTestContainerInitializer;
import com.neighbor.eventmosaic.processor.testcontainer.RedisTestContainerInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class BatchProcessingSchedulerTest implements RedisTestContainerInitializer, KafkaTestContainerInitializer {

    @Autowired
    private BatchProcessingScheduler scheduler;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @MockitoSpyBean
    private BatchStateService batchStateService;

    @MockitoSpyBean
    private EventProcessingService eventProcessingService;

    @MockitoSpyBean
    private KafkaMessagePublisher kafkaMessagePublisher;

    @MockitoSpyBean
    private RedisBatchCleaner redisBatchCleaner;

    private static final String TEST_BATCH_ID = "20250323151500";
    private static final Long TEST_EVENT_ID = 123456L;
    private static final String TEST_MENTION_ID = "789012";

    private ElasticEvent testEvent;
    private ElasticMention testMention;
    private BatchData testBatchData;

    @BeforeEach
    void setUp() {
        // Очищаем Redis перед каждым тестом
        Objects.requireNonNull(redisTemplate.getConnectionFactory())
                .getConnection()
                .serverCommands()
                .flushDb();

        testEvent = new ElasticEvent();
        testEvent.setGlobalEventId(TEST_EVENT_ID);

        testMention = new ElasticMention();
        testMention.setGlobalEventId(TEST_EVENT_ID);
        testMention.setMentionIdentifier(TEST_MENTION_ID);

        testBatchData = new BatchData(
                List.of(testEvent),
                List.of(testMention)
        );
    }

    @Test
    @DisplayName("checkBatchWindows должен вызывать сервис проверки и не выполнять другие действия")
    void checkBatchWindows_shouldCallBatchStateService() {
        // Arrange
        doReturn(0)
                .when(batchStateService)
                .checkExpiredBatchWindows();

        // Act
        scheduler.checkBatchWindows();

        // Assert
        verify(batchStateService, times(1)).checkExpiredBatchWindows();
        verify(eventProcessingService, never()).processBatch(anyString());
        verify(kafkaMessagePublisher, never()).sendEvent(any(), anyString());
        verify(kafkaMessagePublisher, never()).sendMention(any(), anyString());
        verify(redisBatchCleaner, never()).cleanupBatch(anyString());
    }

    @Test
    @DisplayName("processBatchIfReady должен завершиться, если нет готовых батчей")
    void processBatchIfReady_shouldDoNothingWhenNoReadyBatches() {
        // Arrange
        doReturn(null)
                .when(batchStateService)
                .getNextReadyBatch();

        // Act
        scheduler.processBatchIfReady();

        // Assert
        verify(batchStateService, times(1)).getNextReadyBatch();
        verify(eventProcessingService, never()).processBatch(anyString());
        verify(kafkaMessagePublisher, never()).sendEvent(any(), anyString());
        verify(kafkaMessagePublisher, never()).sendMention(any(), anyString());
        verify(redisBatchCleaner, never()).cleanupBatch(anyString());
    }

    @Test
    @DisplayName("processBatchIfReady должен обработать батч и отправить данные")
    void processBatchIfReady_shouldProcessBatchAndSendData() {
        // Arrange
        doReturn(TEST_BATCH_ID)
                .when(batchStateService)
                .getNextReadyBatch();
        doReturn(testBatchData)
                .when(eventProcessingService)
                .processBatch(TEST_BATCH_ID);

        var futureResult = CompletableFuture.completedFuture(mock(SendResult.class));
        doReturn(futureResult)
                .when(kafkaMessagePublisher)
                .sendEvent(any(ElasticEvent.class), anyString());
        doReturn(futureResult)
                .when(kafkaMessagePublisher)
                .sendMention(any(ElasticMention.class), anyString());

        // Act
        scheduler.processBatchIfReady();

        // Assert
        verify(batchStateService, times(1)).getNextReadyBatch();
        verify(eventProcessingService, times(1)).processBatch(TEST_BATCH_ID);
        verify(kafkaMessagePublisher, times(1)).sendEvent(testEvent, testEvent.getGlobalEventId().toString());
        verify(kafkaMessagePublisher, times(1)).sendMention(
                testMention,
                testMention.getGlobalEventId() + "_" + testMention.getMentionIdentifier());

        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(redisBatchCleaner, times(1)).cleanupBatch(TEST_BATCH_ID));
    }

    @Test
    @DisplayName("processBatchIfReady должен обрабатывать ошибки при обработке батча")
    void processBatchIfReady_shouldHandleProcessingErrors() {
        // Arrange
        doReturn(TEST_BATCH_ID)
                .when(batchStateService)
                .getNextReadyBatch();
        doThrow(new RuntimeException("Ошибка обработки"))
                .when(eventProcessingService)
                .processBatch(TEST_BATCH_ID);

        // Act
        scheduler.processBatchIfReady();

        // Assert
        verify(batchStateService, times(1)).getNextReadyBatch();
        verify(eventProcessingService, times(1)).processBatch(TEST_BATCH_ID);
        verify(kafkaMessagePublisher, never()).sendEvent(any(), anyString());
        verify(kafkaMessagePublisher, never()).sendMention(any(), anyString());
        verify(redisBatchCleaner, never()).cleanupBatch(anyString());
    }

    @Test
    @DisplayName("processBatchIfReady должен обрабатывать ошибки при отправке в Kafka")
    void processBatchIfReady_shouldHandleKafkaSendErrors() {
        // Arrange
        doReturn(TEST_BATCH_ID)
                .when(batchStateService)
                .getNextReadyBatch();
        doReturn(testBatchData)
                .when(eventProcessingService)
                .processBatch(TEST_BATCH_ID);

        var failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Ошибка отправки в Kafka"));
        doReturn(failedFuture)
                .when(kafkaMessagePublisher)
                .sendEvent(any(ElasticEvent.class), anyString());

        var successFuture = CompletableFuture.completedFuture(mock(SendResult.class));
        doReturn(successFuture)
                .when(kafkaMessagePublisher)
                .sendMention(any(ElasticMention.class), anyString());

        // Act
        scheduler.processBatchIfReady();

        // Assert
        verify(batchStateService, times(1)).getNextReadyBatch();
        verify(eventProcessingService, times(1)).processBatch(TEST_BATCH_ID);
        verify(kafkaMessagePublisher, times(1)).sendEvent(any(), anyString());
        verify(kafkaMessagePublisher, times(1)).sendMention(any(), anyString());

        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(redisBatchCleaner, never()).cleanupBatch(TEST_BATCH_ID));
    }

    @Test
    @DisplayName("processBatchIfReady должен обрабатывать батч с пустыми данными")
    void processBatchIfReady_shouldHandleEmptyBatchData() {
        // Arrange
        doReturn(TEST_BATCH_ID)
                .when(batchStateService)
                .getNextReadyBatch();
        doReturn(new BatchData(Collections.emptyList(), Collections.emptyList()))
                .when(eventProcessingService)
                .processBatch(TEST_BATCH_ID);

        // Act
        scheduler.processBatchIfReady();

        // Assert
        verify(batchStateService, times(1)).getNextReadyBatch();
        verify(eventProcessingService, times(1)).processBatch(TEST_BATCH_ID);
        verify(kafkaMessagePublisher, never()).sendEvent(any(), anyString());
        verify(kafkaMessagePublisher, never()).sendMention(any(), anyString());

        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(redisBatchCleaner, times(1)).cleanupBatch(TEST_BATCH_ID));
    }
}