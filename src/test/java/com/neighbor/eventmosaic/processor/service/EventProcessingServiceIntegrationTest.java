package com.neighbor.eventmosaic.processor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neighbor.eventmosaic.library.common.dto.Event;
import com.neighbor.eventmosaic.library.common.dto.Mention;
import com.neighbor.eventmosaic.processor.component.BatchProcessor;
import com.neighbor.eventmosaic.processor.dto.BatchData;
import com.neighbor.eventmosaic.processor.dto.ElasticEvent;
import com.neighbor.eventmosaic.processor.dto.ElasticMention;
import com.neighbor.eventmosaic.processor.exception.RedisOperationException;
import com.neighbor.eventmosaic.processor.exception.RedisSerializationException;
import com.neighbor.eventmosaic.processor.mapper.EventMapper;
import com.neighbor.eventmosaic.processor.mapper.MentionMapper;
import com.neighbor.eventmosaic.processor.testcontainer.RedisTestContainerInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class EventProcessingServiceIntegrationTest implements RedisTestContainerInitializer {

    @Autowired
    private EventProcessingService eventProcessingService;

    @Autowired
    EventMapper eventMapper;

    @Autowired
    MentionMapper mentionMapper;

    @MockitoSpyBean
    private RedisTemplate<String, String> redisTemplate;

    @MockitoSpyBean
    private ObjectMapper objectMapper;

    @MockitoSpyBean
    private BatchProcessor batchProcessor;

    private static final String TEST_BATCH_ID = "20250323151500";
    private static final Long TEST_EVENT_ID = 123456L;
    private static final String TEST_MENTION_ID = "123456_789012";

    @BeforeEach
    void setUp() {
        // Очищаем Redis перед каждым тестом
        Objects.requireNonNull(redisTemplate.getConnectionFactory())
                .getConnection()
                .serverCommands()
                .flushDb();
    }

    @Test
    @DisplayName("Должен корректно сохранять событие в Redis")
    void storeEvent_shouldSaveEventToRedis() throws JsonProcessingException {
        // Arrange
        Event event = createEvent(TEST_EVENT_ID, 20250323, 10.5);

        // Act
        eventProcessingService.storeEvent(TEST_BATCH_ID, event);

        // Assert
        String eventKey = "data:event:" + TEST_BATCH_ID + ":" + TEST_EVENT_ID;
        String setKey = "batch:events:" + TEST_BATCH_ID;

        // Проверяем наличие ключей в Redis
        assertThat(redisTemplate.hasKey(eventKey)).isTrue();
        assertThat(redisTemplate.hasKey(setKey)).isTrue();

        // Проверяем содержимое ключей
        String storedJson = redisTemplate.opsForValue().get(eventKey);
        assertThat(storedJson).isNotNull();
        Event storedEvent = objectMapper.readValue(storedJson, Event.class);
        assertThat(storedEvent).usingRecursiveComparison().isEqualTo(event);

        // Проверяем, что ID события добавлен в set событий батча
        Set<String> eventIds = redisTemplate.opsForSet().members(setKey);
        assertThat(eventIds).contains(TEST_EVENT_ID.toString());
    }

    @Test
    @DisplayName("Должен корректно сохранять упоминание в Redis")
    void storeMention_shouldSaveMentionToRedis() throws JsonProcessingException {
        // Arrange
        Mention mention = createMention(TEST_EVENT_ID, "789012", -35.0);

        // Act
        eventProcessingService.storeMention(TEST_BATCH_ID, mention);

        // Assert
        String mentionKey = "data:mention:" + TEST_BATCH_ID + ":" + TEST_MENTION_ID;
        String setKey = "batch:mentions:" + TEST_BATCH_ID;

        assertThat(redisTemplate.hasKey(mentionKey)).isTrue();
        assertThat(redisTemplate.hasKey(setKey)).isTrue();

        String storedJson = redisTemplate.opsForValue().get(mentionKey);
        assertThat(storedJson).isNotNull();
        Mention storedMention = objectMapper.readValue(storedJson, Mention.class);
        assertThat(storedMention).usingRecursiveComparison().isEqualTo(mention);

        Set<String> mentionIds = redisTemplate.opsForSet().members(setKey);
        assertThat(mentionIds).contains(TEST_MENTION_ID);
    }

    @Test
    @DisplayName("Должен выбрасывать RedisSerializationException при ошибке сериализации")
    void storeEvent_shouldThrowRedisSerializationException() throws JsonProcessingException {
        // Arrange
        Event event = mock(Event.class);
        when(event.getGlobalEventId()).thenReturn(TEST_EVENT_ID);

        doThrow(JsonProcessingException.class).when(objectMapper).writeValueAsString(any(Event.class));

        // Act & Assert
        assertThatThrownBy(() -> eventProcessingService.storeEvent(TEST_BATCH_ID, event))
                .isInstanceOf(RedisSerializationException.class)
                .hasMessageContaining("Ошибка сериализации объекта для Redis");
    }

    @Test
    @DisplayName("Должен выбрасывать RedisOperationException при ошибке операций с Redis")
    void storeEvent_shouldThrowRedisOperationException() {
        // Arrange
        Event event = createEvent(TEST_EVENT_ID, null, null);

        doThrow(RuntimeException.class).when(redisTemplate).opsForValue();

        // Act & Assert
        assertThatThrownBy(() -> eventProcessingService.storeEvent(TEST_BATCH_ID, event))
                .isInstanceOf(RedisOperationException.class)
                .hasMessageContaining("Ошибка сохранения объекта в Redis");
    }

    @Test
    @DisplayName("processBatch должен извлекать данные из Redis и обрабатывать их")
    void processBatch_shouldRetrieveAndProcessData() {
        // Arrange
        Event event = createEvent(TEST_EVENT_ID, 20250323, 10.5);
        Mention mention = createMention(TEST_EVENT_ID, "789012", -35.0);

        eventProcessingService.storeEvent(TEST_BATCH_ID, event);
        eventProcessingService.storeMention(TEST_BATCH_ID, mention);

        List<ElasticEvent> expectedEvents = eventMapper.toElasticEvents(List.of(event));
        List<ElasticMention> expectedMentions = mentionMapper.toElasticMentionList(List.of(mention));
        BatchData expectedResult = new BatchData(expectedEvents, expectedMentions);

        doReturn(expectedResult).when(batchProcessor).process(any(), any());

        // Act
        BatchData result = eventProcessingService.processBatch(TEST_BATCH_ID);

        // Assert
        verify(batchProcessor).process(expectedEvents, expectedMentions);

        assertThat(result).isNotNull();
        assertThat(result.getEvents()).hasSize(1);
        assertThat(result.getMentions()).hasSize(1);
        assertThat(result.getEvents().getFirst().getGlobalEventId()).isEqualTo(TEST_EVENT_ID);
        assertThat(result.getMentions().getFirst().getGlobalEventId()).isEqualTo(TEST_EVENT_ID);
    }

    @Test
    @DisplayName("processBatch должен возвращать пустые списки, если в Redis нет данных")
    void processBatch_shouldReturnEmptyListsWhenNoDataInRedis() {
        // Arrange
        BatchData expectedEmptyResult = new BatchData(Collections.emptyList(), Collections.emptyList());
        doReturn(expectedEmptyResult).when(batchProcessor).process(any(), any());

        // Act
        BatchData result = eventProcessingService.processBatch(TEST_BATCH_ID);

        // Assert
        verify(batchProcessor).process(Collections.emptyList(), Collections.emptyList());
        assertThat(result).isNotNull();
        assertThat(result.getEvents()).isEmpty();
        assertThat(result.getMentions()).isEmpty();
    }

    @Test
    @DisplayName("Должен обрабатывать исключение при десериализации данных")
    void processBatch_shouldHandleDeserializationErrors() {
        // Arrange
        String eventKey = "data:event:" + TEST_BATCH_ID + ":" + TEST_EVENT_ID;
        String setKey = "batch:events:" + TEST_BATCH_ID;

        redisTemplate.opsForValue().set(eventKey, "invalid-json");
        redisTemplate.opsForSet().add(setKey, TEST_EVENT_ID.toString());

        BatchData expectedEmptyResult = new BatchData(Collections.emptyList(), Collections.emptyList());
        doReturn(expectedEmptyResult).when(batchProcessor).process(any(), any());

        // Act
        BatchData result = eventProcessingService.processBatch(TEST_BATCH_ID);

        // Assert
        verify(batchProcessor).process(Collections.emptyList(), Collections.emptyList());
        assertThat(result).isNotNull();
        assertThat(result.getEvents()).isEmpty();
        assertThat(result.getMentions()).isEmpty();
    }


    private Event createEvent(Long id,
                              Integer day,
                              Double avgTone) {
        Event event = new Event();
        event.setGlobalEventId(id);
        event.setDay(day);
        event.setAvgTone(avgTone);
        return event;
    }

    private Mention createMention(Long eventId,
                                  String mentionId,
                                  Double docTone) {
        Mention mention = new Mention();
        mention.setGlobalEventId(eventId);
        mention.setMentionIdentifier(mentionId);
        mention.setMentionDocTone(docTone);
        return mention;
    }
}
