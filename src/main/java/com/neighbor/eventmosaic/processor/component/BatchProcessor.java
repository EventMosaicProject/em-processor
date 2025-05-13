package com.neighbor.eventmosaic.processor.component;

import com.neighbor.eventmosaic.library.common.dto.Event;
import com.neighbor.eventmosaic.library.common.dto.Mention;
import com.neighbor.eventmosaic.processor.dto.BatchData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchProcessor {

    public BatchData process(List<Event> events,
                             List<Mention> mentions) {

        try {
            // При необходимости добавить логику обработки данных, например, фильтрация, агрегация и тд.
            // Упоминания могут содержать идентификатор события которого нет в файле событий текущего батча

            return new BatchData(events, mentions);

        } catch (Exception e) {
            log.error("Ошибка при обработке батча: {}", e.getMessage(), e);
            // Возвращаем исходные данные, если что-то пошло не так
            return new BatchData(events, mentions);
        }
    }
}
