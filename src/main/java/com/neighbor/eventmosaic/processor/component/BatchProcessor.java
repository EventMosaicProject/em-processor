package com.neighbor.eventmosaic.processor.component;

import com.neighbor.eventmosaic.library.common.dto.Event;
import com.neighbor.eventmosaic.library.common.dto.Mention;
import com.neighbor.eventmosaic.processor.dto.BatchData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchProcessor {

    public BatchData process(List<Event> events,
                             List<Mention> mentions) {

        try {
            // TODO: добавить логику обработки данных, например, фильтрация, агрегация и тд.

            // Фильтрация событий (например только события с определённым условием)
            List<Event> filteredEvents = events.stream()
                    .filter(event -> event.getGlobalEventId() != null)
                    .toList();

            // Собираем ID оставшихся событий
            Set<Long> validEventIds = filteredEvents.stream()
                    .map(Event::getGlobalEventId)
                    .collect(Collectors.toSet());

            // Фильтрация упоминаний: только те, что ссылаются на оставшиеся события
            List<Mention> filteredMentions = mentions.stream()
                    .filter(mention -> validEventIds.contains(mention.getGlobalEventId()))
                    .toList();

            log.info("Обработка батча: {} событий, {} упоминаний после фильтрации",
                    filteredEvents.size(), filteredMentions.size());

            return new BatchData(filteredEvents, filteredMentions);

        } catch (Exception e) {
            log.error("Ошибка при обработке батча: {}", e.getMessage(), e);
            // Возвращаем исходные данные, если что-то пошло не так
            return new BatchData(events, mentions);
        }
    }
}
